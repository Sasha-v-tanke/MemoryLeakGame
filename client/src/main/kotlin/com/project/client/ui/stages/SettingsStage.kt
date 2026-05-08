package com.project.client.ui.stages

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.MainScreen
import com.project.client.ui.theme.UiTheme

class SettingsStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    override fun buildUI() {
        val root = screenRoot()
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(620f)

        val title = titleLabel("Настройки", 1.30f).apply {
            setAlignment(Align.center)
        }

        val text = subtitleLabel(
            "Управление:\n" +
                    "WASD / Стрелки — двигать камеру\n" +
                    "Q / E — масштабирование\n" +
                    "Клик на карту — выбрать карту\n" +
                    "Клик на арену — если выбрана карта\n"
        ).apply {
            wrap = true
        }

        val backButton = TextButton("Назад", skin)
        UiTheme.styleSecondaryButton(backButton)

        box.defaults().pad(8f)
        box.add(title).growX().row()
        box.add(text).width(560f).padBottom(18f).row()
        box.add(backButton).height(42f).growX().row()

        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(MainScreen(game))
                true
            } else {
                false
            }
        }
    }
}
