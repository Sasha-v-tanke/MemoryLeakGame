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
import com.project.client.ui.screens.LoginScreen

class RegisterStage(
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
        root.add(box).width(540f)

        val title = Label("Create Instance Profile", skin).apply {
            setAlignment(Align.center)
            fontScaleX = 1.25f
            fontScaleY = 1.25f
        }

        val usernameField = TextField("", skin).apply { messageText = "username" }
        val emailField = TextField("", skin).apply { messageText = "email" }
        val passwordField = TextField("", skin).apply {
            messageText = "password"
            isPasswordMode = true
            setPasswordCharacter('*')
        }

        val messageLabel = Label("", skin).apply {
            setAlignment(Align.center)
            wrap = true
        }

        val registerButton = TextButton("Sign up", skin)
        val backButton = TextButton("Back to login", skin)

        box.defaults().pad(7f)
        box.add(title).growX().padBottom(16f).row()
        box.add(Label("Username", skin)).left().growX().row()
        box.add(usernameField).height(42f).growX().row()
        box.add(Label("Email", skin)).left().growX().row()
        box.add(emailField).height(42f).growX().row()
        box.add(Label("Password", skin)).left().growX().row()
        box.add(passwordField).height(42f).growX().row()
        box.add(registerButton).height(44f).growX().padTop(12f).row()
        box.add(backButton).height(38f).growX().row()
        box.add(messageLabel).width(460f).padTop(10f).row()

        registerButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                val username = usernameField.text.trim()
                val email = emailField.text.trim()
                val password = passwordField.text

                messageLabel.setText("Creating account...")

                authSocket.register(username, password, email) { response ->
                    if (response.success) {
                        messageLabel.setText("Account created. You can login now.")
                    } else {
                        messageLabel.setText(response.message)
                    }
                }

                true
            } else {
                false
            }
        }

        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(LoginScreen(game))
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
