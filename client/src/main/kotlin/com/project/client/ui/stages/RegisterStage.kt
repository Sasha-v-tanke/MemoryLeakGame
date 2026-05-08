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
import com.project.client.ui.theme.UiTheme

class RegisterStage(
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

        val title = titleLabel("Создать профиль экземпляра", 1.30f).apply {
            setAlignment(Align.center)
        }

        val usernameField = TextField("", skin).apply { messageText = "логин" }
        val emailField = TextField("", skin).apply { messageText = "email" }
        val passwordField = TextField("", skin).apply {
            messageText = "пароль"
            isPasswordMode = true
            setPasswordCharacter('*')
        }

        val messageLabel = Label("", skin).apply {
            setAlignment(Align.center)
            wrap = true
            color = UiTheme.statusInfo
        }

        val registerButton = TextButton("Регистрация", skin)
        val backButton = TextButton("Назад к входу", skin)
        UiTheme.stylePrimaryButton(registerButton)
        UiTheme.styleSecondaryButton(backButton)

        box.defaults().pad(7f)
        box.add(title).growX().padBottom(16f).row()
        box.add(mutedLabel("Логин", 0.95f)).left().growX().row()
        box.add(usernameField).height(42f).growX().row()
        box.add(mutedLabel("Email", 0.95f)).left().growX().row()
        box.add(emailField).height(42f).growX().row()
        box.add(mutedLabel("Пароль", 0.95f)).left().growX().row()
        box.add(passwordField).height(42f).growX().row()
        box.add(registerButton).height(44f).growX().padTop(12f).row()
        box.add(backButton).height(38f).growX().row()
        box.add(messageLabel).width(540f).padTop(10f).row()

        registerButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                val username = usernameField.text.trim()
                val email = emailField.text.trim()
                val password = passwordField.text

                if (username.isBlank() || email.isBlank() || password.isBlank()) {
                    messageLabel.setText("Заполните логин, email и пароль.")
                    messageLabel.color = UiTheme.statusError
                    return@addListener true
                }

                messageLabel.setText("Создание аккаунта...")
                messageLabel.color = UiTheme.statusInfo

                authSocket.register(username, password, email) { response ->
                    if (response.success) {
                        messageLabel.setText("Аккаунт создан. Теперь вы можете войти.")
                        messageLabel.color = UiTheme.statusOk
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
