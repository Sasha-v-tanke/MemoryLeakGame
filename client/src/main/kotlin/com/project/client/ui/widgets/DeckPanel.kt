package com.project.client.ui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.project.client.ui.theme.UiTheme
import com.project.shared.engine.entities.components.FactoryType
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitRole
import com.project.shared.engine.entities.units.UnitType

typealias OnCardSelected = (unitType: UnitType) -> Unit

class DeckPanel(
    skin: Skin,
    private val playerDeck: List<UnitType>,
    private val onCardSelected: OnCardSelected
) : Table(skin) {
    private val cardButtons = mutableMapOf<UnitType, MutableList<UnitCardButton>>()

    init {
        background = skin.newDrawable("default-round", Color(0.020f, 0.032f, 0.060f, 0.94f))
        pad(10f)

        val header = Label("Deck Runtime: cooldown and factory queue", skin, "small").apply {
            setAlignment(Align.left)
            color = UiTheme.statusInfo
        }

        val cardsTable = Table(skin).apply {
            top().left()
            defaults().pad(4f)
        }

        playerDeck.forEach { unitType ->
            val config = UnitRegistry.getConfig(unitType)
            val cardButton = UnitCardButton(config, skin)
            cardButtons.getOrPut(unitType) { mutableListOf() } += cardButton

            cardButton.addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    onCardSelected(unitType)
                }
            })

            cardsTable.add(cardButton).width(144f).height(112f)
        }

        add(header).left().growX().padBottom(6f).row()
        add(cardsTable).grow()
    }

    fun setSelectedCard(unitType: UnitType?) {
        cardButtons.values.flatten().forEach { button ->
            button.setSelected(false)
        }

        if (unitType != null) {
            cardButtons[unitType]?.forEach { button ->
                button.setSelected(true)
            }
        }
    }

    fun updateRuntime(
        cooldownByCardMs: Map<UnitType, Long>,
        queueSizesByFactory: Map<FactoryType, Int>
    ) {
        cardButtons.forEach { (unitType, buttons) ->
            val config = UnitRegistry.getConfig(unitType)
            val cooldown = cooldownByCardMs[unitType] ?: 0L
            val queueSize = when (config.role) {
                UnitRole.SPELL -> 0
                else -> {
                    val factoryType = requiredFactoryFor(unitType)
                    queueSizesByFactory[factoryType] ?: 0
                }
            }

            buttons.forEach { it.updateRuntime(cooldown, queueSize) }
        }
    }

    private fun requiredFactoryFor(unitType: UnitType): FactoryType {
        return when (unitType) {
            UnitType.ALLOCATOR,
            UnitType.INJECTOR,
            UnitType.CACHE_RUNNER,
            UnitType.COROUTINE_ARCHER -> FactoryType.BASIC

            UnitType.GARBAGE_COLLECTOR,
            UnitType.THREAD_GUARD,
            UnitType.FIREWALL,
            UnitType.PATCH_HEALER,
            UnitType.DEADLOCK,
            UnitType.OVERCLOCK -> FactoryType.SUPPORT
        }
    }
}
