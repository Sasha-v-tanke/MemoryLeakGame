package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.engine.GameConfig
import com.project.client.ui.screens.MainScreen

class UIStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {

    private lateinit var waitingMessage: Label
    private lateinit var waitingBox: Table

    override fun buildUI() {
        val table = Table()
        table.setFillParent(true)
        table.top().left()
        addActor(table)

        val backButton = TextButton("Back", skin)
        table.add(backButton).pad(10f)

        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.screen = MainScreen(game)
                true
            } else false
        }

        val centerTable = Table().apply {
            setFillParent(true)
            center()
        }
        addActor(centerTable)

        waitingMessage = Label("Waiting...", skin).apply {
            setAlignment(Align.center)
        }

        waitingBox = Table(skin).apply {
            background = skin.newDrawable("default-round", Color(0f, 0f, 0f, 0.65f))
            add(waitingMessage).pad(16f)
        }

        centerTable.add(waitingBox)
    }

    fun startGame() {
        waitingBox.isVisible = false
    }
}