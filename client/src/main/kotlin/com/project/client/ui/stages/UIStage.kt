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
import com.project.client.ui.widgets.DeckPanel
import com.project.shared.engine.entities.units.UnitType

class UIStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    private lateinit var waitingBox: Table
    private lateinit var centerTable: Table
    private lateinit var leftTopTable: Table
    private lateinit var hoverInfoBox: Table
    private lateinit var hoverInfoLabel: Label
    private lateinit var bottomTable: Table

    override fun buildUI() {
        setupTables()

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
            pad(40f)
        }
        leftTopTable.add(hoverInfoBox)
        hoverInfoLabel = Label("", skin).apply {
            setAlignment(Align.left)
        }
        hoverInfoBox.add(hoverInfoLabel)
        hoverInfoBox.isVisible = false
        addActor(hoverInfoBox)

        val waitingMessage = Label("Waiting...", skin).apply {
            setAlignment(Align.center)
        }
        waitingBox = Table(skin).apply {
            background = skin.newDrawable("default-round", Color(0f, 0f, 0f, 0.65f))
            add(waitingMessage).pad(16f)
        }
        centerTable.add(waitingBox)

        val playerDeck = listOf(
            UnitType.ALLOCATOR,
            UnitType.DEADLOCK,
            UnitType.GARBAGE_COLLECTOR,
            UnitType.ALLOCATOR,
            UnitType.INJECTOR
        )

        val deckPanel = DeckPanel(skin, playerDeck) { unitType ->
            onDeckCardSelected(unitType)
        }

        bottomTable.add(deckPanel).width(600f).height(100f)
    }

    fun setupTables() {
        leftTopTable = Table().apply {
            setFillParent(true)
            top()
            left()
        }
        addActor(leftTopTable)

        centerTable = Table().apply {
            setFillParent(true)
            center()
        }
        addActor(centerTable)

        bottomTable = Table().apply {
            setFillParent(true)
            bottom()
        }
        addActor(bottomTable)
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


    fun onDeckCardSelected(unitType: UnitType) {
        // Handle card selection, e.g., send a command to the server to spawn the unit
        println("Selected card: $unitType")
    }

}