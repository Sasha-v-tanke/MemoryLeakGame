package com.project.client.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.api.AuthWebSocket
import com.project.client.screens.RegisterScreen
import com.project.client.stages.BaseStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val authSocket = AuthWebSocket("ws://localhost:8080/user/login")

    override fun show() {
        scope.launch { authSocket.connect() }
        super.show()
    }

    override fun buildUI() {

        val table = Table()
        table.center()
        table.setFillParent(true)
        table.defaults().pad(6f)
        addActor(table)

        val usernameField = TextField("", skin)
        val passwordField = TextField("", skin).apply {
            isPasswordMode = true
        }

        val messageLabel = Label("", skin).apply {
            setAlignment(Align.center)
            wrap = true
        }

        table.center()

        table.row()
        table.add(Label("Login", skin)).colspan(2).padBottom(16f)

        table.row()
        table.add(Label("Username:", skin))
        table.add(usernameField).width(260f)

        table.row()
        table.add(Label("Password:", skin))
        table.add(passwordField).width(260f)

        table.row()
        val loginButton = TextButton("Login", skin)
        table.add(loginButton).colspan(2)

        table.row()
        val switchButton = TextButton("Register", skin)
        table.add(switchButton).colspan(2)

        table.row()
        table.add(messageLabel).colspan(2).width(400f)

        loginButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                val username = usernameField.text
                val password = passwordField.text

                authSocket.login(username, password) { response ->
                    messageLabel.setText(if (response.success) "Success" else response.message ?: "Error")
                }
                true
            } else {
                false
            }
        }

        switchButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(RegisterScreen(game))
                true
            } else {
                false
            }
        }
    }
}