package com.project.client.ui.stages

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.MainScreen

class SettingsStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    override fun buildUI() {
        val root = Table()
        root.setFillParent(true)
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(560f)

        val title = Label("Settings", skin).apply {
            setAlignment(Align.center)
            fontScaleX = 1.25f
            fontScaleY = 1.25f
        }

        val text = Label(
            "Controls:\n" +
                    "WASD / Arrows — move camera\n" +
                    "Q / E — zoom\n" +
                    "Click card — select card\n" +
                    "Click arena — deploy selected card\n\n",
            skin
        ).apply {
            wrap = true
        }

        val backButton = TextButton("Back", skin)

        box.defaults().pad(8f)
        box.add(title).growX().row()
        box.add(text).width(500f).padBottom(18f).row()
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
