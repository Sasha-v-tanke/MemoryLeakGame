package com.project.client.ui.stages

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.MainScreen

class UIStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {

    private lateinit var waitingMessage: Label
    private lateinit var waitingBox: Table
    private lateinit var centerTable: Table
    private lateinit var leftTopTable: Table
    private lateinit var hoverInfoBox: Table
    private lateinit var hoverInfoLabel: Label

    override fun buildUI() {
        leftTopTable = Table().apply {
            setFillParent(true)
            top()
            left()
        }
        addActor(leftTopTable)

        val backButton = TextButton("Back", skin)
        leftTopTable.add(backButton).pad(10f)

        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.screen = MainScreen(game)
                true
            } else false
        }

        hoverInfoBox = Table().apply {
            setFillParent(true)
            top()
            left()
            pad(20f)
        }
        leftTopTable.add(hoverInfoBox)
        hoverInfoLabel = Label("", skin).apply {
            setAlignment(Align.left)
        }
        hoverInfoBox.add(hoverInfoLabel)
        hoverInfoBox.isVisible = false
        addActor(hoverInfoBox)

        centerTable = Table().apply {
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


    fun showHoverInfo(text: String) {
        hoverInfoLabel.setText(text)
        hoverInfoBox.isVisible = true
    }

    fun hideHoverInfo() {
        hoverInfoBox.isVisible = false
    }
}