package com.project.client.ui.stages

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.LoginScreen
import com.project.client.ui.screens.MainScreen

class GameStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    lateinit var backButton: TextButton

    override fun buildUI() {
        val table = Table()
        table.setFillParent(true)
        table.top().left()
        addActor(table)

        backButton = TextButton("Back", skin)
        table.add(backButton).pad(10f)
        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.screen = MainScreen(game)
                true
            } else false
        }
    }

    override fun show() {

    }
}