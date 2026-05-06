package com.project.client.ui.stages

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.network.api.AuthSocket
import com.project.client.ui.screens.MainScreen
import com.project.client.ui.screens.RegisterScreen

class LoginStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    private val authSocket = AuthSocket()

    override fun buildUI() {
        val root = Table()
        root.setFillParent(true)
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(520f)

        val title = Label("Memory Leak Arena", skin).apply {
            setAlignment(Align.center)
            fontScaleX = 1.35f
            fontScaleY = 1.35f
        }

        val subtitle = Label("1v1 Card RTS · Systems Battle Simulator", skin).apply {
            setAlignment(Align.center)
        }

        val usernameField = TextField("", skin)
        usernameField.messageText = "username"

        val passwordField = TextField("", skin).apply {
            messageText = "password"
            isPasswordMode = true
            setPasswordCharacter('*')
        }

        val messageLabel = Label("", skin).apply {
            setAlignment(Align.center)
            wrap = true
        }

        val loginButton = TextButton("Login", skin)
        val registerButton = TextButton("Create account", skin)

        box.defaults().pad(7f)
        box.add(title).growX().row()
        box.add(subtitle).growX().padBottom(18f).row()
        box.add(Label("Username", skin)).left().growX().row()
        box.add(usernameField).height(42f).growX().row()
        box.add(Label("Password", skin)).left().growX().row()
        box.add(passwordField).height(42f).growX().row()
        box.add(loginButton).height(44f).growX().padTop(12f).row()
        box.add(registerButton).height(38f).growX().row()
        box.add(messageLabel).width(460f).padTop(10f).row()

        loginButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                val username = usernameField.text.trim()
                val password = passwordField.text

                messageLabel.setText("Connecting...")

                authSocket.login(username, password) { response ->
                    val playerId = response.playerId

                    if (response.success && playerId != null) {
                        game.setProfile(playerId, username)
                        game.setScreen(MainScreen(game))
                    } else {
                        messageLabel.setText(response.message)
                    }
                }

                true
            } else {
                false
            }
        }

        registerButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(RegisterScreen(game))
                true
            } else {
                false
            }
        }
    }

    override fun dispose() {
        authSocket.close()
        super.dispose()
    }
}
