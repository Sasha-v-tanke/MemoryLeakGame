package com.project.client.ui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitType

typealias OnCardSelected = (unitType: UnitType) -> Unit

class DeckPanel(
    skin: Skin,
    private val playerDeck: List<UnitType>,
    private val onCardSelected: OnCardSelected
) : Table(skin) {
    init {
        background = skin.newDrawable("default-round", Color(0.025f, 0.035f, 0.065f, 0.92f))
        pad(8f)

        val scrollTable = Table(skin).apply {
            left()
        }

        playerDeck.forEach { unitType ->
            val config = UnitRegistry.getConfig(unitType)
            val cardButton = UnitCardButton(config, skin)

            cardButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    onCardSelected(unitType)
                }
            })

            scrollTable.add(cardButton).width(138f).height(106f).padRight(8f)
        }

        val scroll = ScrollPane(scrollTable, skin)
        scroll.setOverscroll(false, false)
        scroll.setFadeScrollBars(false)

        add(scroll).grow()
    }
}
