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
import com.project.client.ui.theme.UiTheme

class LoginStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    private val authSocket = AuthSocket()

    override fun buildUI() {
        val root = screenRoot()
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(620f)

        val title = titleLabel("Memory Leak Arena").apply {
            setAlignment(Align.center)
        }

        val subtitle = subtitleLabel("Desktop 1v1 Card RTS · Systems Architecture Training").apply {
            setAlignment(Align.center)
        }

        val versionLine = mutedLabel("Build focus: Memory / CPU / Concurrency mechanics", 0.95f).apply {
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
            color = UiTheme.statusInfo
        }

        val loginButton = TextButton("Login", skin)
        val registerButton = TextButton("Create account", skin)
        UiTheme.stylePrimaryButton(loginButton)
        UiTheme.styleSecondaryButton(registerButton)

        box.defaults().pad(7f)
        box.add(title).growX().row()
        box.add(subtitle).growX().padTop(2f).row()
        box.add(versionLine).growX().padBottom(18f).row()
        box.add(mutedLabel("Username", 0.95f)).left().growX().row()
        box.add(usernameField).height(42f).growX().row()
        box.add(mutedLabel("Password", 0.95f)).left().growX().row()
        box.add(passwordField).height(42f).growX().row()
        box.add(loginButton).height(44f).growX().padTop(12f).row()
        box.add(registerButton).height(38f).growX().row()
        box.add(messageLabel).width(540f).padTop(10f).row()

        loginButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                val username = usernameField.text.trim()
                val password = passwordField.text

                if (username.isBlank() || password.isBlank()) {
                    messageLabel.setText("Enter username and password.")
                    messageLabel.color = UiTheme.statusError
                    return@addListener true
                }

                messageLabel.setText("Connecting...")
                messageLabel.color = UiTheme.statusInfo

                authSocket.login(username, password) { response ->
                    val playerId = response.playerId

                    if (response.success && playerId != null) {
                        game.setProfile(playerId, username)
                        game.setScreen(MainScreen(game))
                    } else {
                        messageLabel.setText(response.message)
                        messageLabel.color = UiTheme.statusError
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
