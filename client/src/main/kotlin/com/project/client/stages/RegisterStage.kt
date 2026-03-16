package com.project.client.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.api.AuthWebSocket
import com.project.client.screens.LoginScreen
import com.project.client.stages.BaseStage
import kotlinx.coroutines.*

class RegisterStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val authSocket = AuthWebSocket("ws://localhost:8080/user/register")

    override fun show() {
        super.show()
        scope.launch { authSocket.connect() }
    }

    override fun buildUI() {

        val table = Table()
        table.setFillParent(true)
        table.center()
        table.defaults().pad(6f)
        addActor(table)

        val usernameField = TextField("", skin)
        val passwordField = TextField("", skin).apply {
            isPasswordMode = true
        }
        val emailField = TextField("", skin)

        val messageLabel = Label("", skin)

        table.center()

        table.row()
        table.add(Label("Register", skin)).colspan(2).padBottom(16f)

        table.row()
        table.add(Label("Username:", skin))
        table.add(usernameField).width(260f)

        table.row()
        table.add(Label("Password:", skin))
        table.add(passwordField).width(260f)

        table.row()
        table.add(Label("Email:", skin))
        table.add(emailField).width(260f)

        table.row()
        val registerButton = TextButton("Sign up", skin)
        table.add(registerButton).colspan(2)

        table.row()
        val switchButton = TextButton("Back to login", skin)
        table.add(switchButton).colspan(2)

        table.row()
        table.add(messageLabel).colspan(2).width(400f)

        registerButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                val username = usernameField.text
                val password = passwordField.text
                val email = emailField.text

                authSocket.register(username, password, email) { response ->
                    messageLabel.setText(if (response.success) "Success" else response.message ?: "Error")
                }
                true
            } else {
                false
            }
        }

        switchButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(LoginScreen(game))
                true
            } else {
                false
            }
        }
    }
}