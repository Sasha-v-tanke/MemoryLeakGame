package com.project.client.ui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.project.shared.engine.gameobjects.UnitType
import com.project.shared.engine.gameobjects.UnitRegistry

typealias OnCardSelected = (unitType: UnitType) -> Unit

class DeckPanel(
    skin: Skin,
    private val playerDeck: List<UnitType>,
    private val onCardSelected: OnCardSelected
) : Table(skin) {

    init {
        background = skin.newDrawable("default-round", Color(0.1f, 0.1f, 0.1f, 0.8f))
        pad(10f)

        val scrollTable = Table(skin).apply {
            left()
        }

        for (unitType in playerDeck) {
            val config = UnitRegistry.getConfig(unitType)
            val cardButton = UnitCardButton(config, skin)

            cardButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    onCardSelected(unitType)
                }
            })

            scrollTable.add(cardButton).fillY().padRight(5f)
        }

        val scroll = ScrollPane(scrollTable, skin)
        scroll.setOverscroll(false, false)
        add(scroll).grow()
    }
}