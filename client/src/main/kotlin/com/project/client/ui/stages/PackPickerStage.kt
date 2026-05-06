package com.project.client.ui.stages

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.MainScreen
import com.project.client.ui.theme.UiTheme
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitRole
import com.project.shared.engine.entities.units.UnitType

class PackPickerStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    private val draftDeck = mutableListOf<UnitType>()
    private val cardCountLabels = mutableMapOf<UnitType, Label>()
    private lateinit var deckSummaryLabel: Label
    private lateinit var messageLabel: Label

    override fun buildUI() {
        draftDeck.clear()
        draftDeck += game.getSelectedDeck()

        val root = screenRoot(20f)
        addActor(root)

        val box = panel()
        root.add(box).grow()

        val title = titleLabel("Deck / Cards", 1.30f).apply {
            setAlignment(Align.center)
        }

        val rulesLabel = subtitleLabel(
            "Rules: exactly ${MyGame.DECK_SIZE} cards · up to ${MyGame.MAX_CARD_COPIES} copies per card",
            0.97f
        ).apply {
            setAlignment(Align.center)
        }

        deckSummaryLabel = Label("", skin).apply {
            setAlignment(Align.center)
            wrap = true
            color = UiTheme.statusInfo
        }

        messageLabel = Label("", skin).apply {
            setAlignment(Align.center)
            wrap = true
            color = UiTheme.statusInfo
        }

        val cardsTable = Table(skin)
        cardsTable.defaults().pad(8f).growX()

        UnitRegistry.all().forEach { config ->
            val card = panel(tint = roleTint(config.role), padding = 12f)
            val countLabel = Label("", skin).apply {
                setAlignment(Align.center)
                color = UiTheme.statusInfo
            }
            cardCountLabels[config.unitType] = countLabel

            val nameLabel = titleLabel(config.displayName, 1.05f)
            val roleLabel = mutedLabel("Role: ${formatRole(config.role)}", 0.94f)
            val statLabel = subtitleLabel(
                "Cost: ${config.costMemory} Memory / ${config.costCpu} CPU · HP ${config.health} · DMG ${config.damage} · SPD ${config.speed.toInt()}",
                0.94f
            )

            card.add(nameLabel).left().row()
            card.add(roleLabel).left().padTop(2f).row()
            card.add(statLabel).left().padTop(2f).row()

            val gameDesc = subtitleLabel(config.gameDescription, 0.95f).apply {
                wrap = true
            }
            val techDesc = mutedLabel("IT: ${config.techDescription}", 0.94f).apply {
                wrap = true
            }

            card.add(gameDesc).width(760f).left().padTop(8f).row()
            card.add(techDesc).width(760f).left().padTop(4f).row()

            val controls = Table(skin)
            val removeButton = TextButton("-", skin)
            val addButton = TextButton("+", skin)
            UiTheme.styleSecondaryButton(removeButton, compact = true)
            UiTheme.stylePrimaryButton(addButton, compact = true)

            controls.add(removeButton).width(42f).height(34f).padRight(8f)
            controls.add(countLabel).width(150f).height(34f).padRight(8f)
            controls.add(addButton).width(42f).height(34f)
            card.add(controls).left().padTop(8f).row()

            removeButton.addListener { event ->
                if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                    removeCard(config.unitType)
                    true
                } else {
                    false
                }
            }

            addButton.addListener { event ->
                if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                    addCard(config.unitType)
                    true
                } else {
                    false
                }
            }

            cardsTable.add(card).growX().row()
        }

        val scroll = ScrollPane(cardsTable, skin)
        scroll.setFadeScrollBars(false)

        val saveButton = TextButton("Save Deck", skin)
        val resetButton = TextButton("Reset Default", skin)
        val backButton = TextButton("Back", skin)
        UiTheme.stylePrimaryButton(saveButton)
        UiTheme.styleSecondaryButton(resetButton)
        UiTheme.styleSecondaryButton(backButton)
        val actions = Table(skin)
        actions.defaults().pad(6f)
        actions.add(saveButton).width(180f).height(42f)
        actions.add(resetButton).width(180f).height(42f)
        actions.add(backButton).width(180f).height(42f)

        box.add(title).growX().padBottom(12f).row()
        box.add(rulesLabel).growX().padBottom(6f).row()
        box.add(deckSummaryLabel).width(840f).padBottom(10f).row()
        box.add(scroll).grow().row()
        box.add(messageLabel).width(840f).height(28f).padTop(8f).row()
        box.add(actions).padTop(4f).row()

        saveButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                saveDeck()
                true
            } else {
                false
            }
        }

        resetButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.resetDeck()
                draftDeck.clear()
                draftDeck += game.getSelectedDeck()
                messageLabel.setText("Default deck restored.")
                messageLabel.color = UiTheme.statusOk
                refreshDeckState()
                true
            } else {
                false
            }
        }

        backButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(MainScreen(game))
                true
            } else {
                false
            }
        }

        refreshDeckState()
    }

    private fun addCard(unitType: UnitType) {
        if (draftDeck.size >= MyGame.DECK_SIZE) {
            messageLabel.setText("Deck is full: ${MyGame.DECK_SIZE} cards.")
            messageLabel.color = UiTheme.statusWarn
            return
        }

        val copies = draftDeck.count { it == unitType }
        if (copies >= MyGame.MAX_CARD_COPIES) {
            messageLabel.setText("Only ${MyGame.MAX_CARD_COPIES} copies of one card are allowed.")
            messageLabel.color = UiTheme.statusWarn
            return
        }

        draftDeck += unitType
        messageLabel.setText("${UnitRegistry.getConfig(unitType).displayName} added.")
        messageLabel.color = UiTheme.statusOk
        refreshDeckState()
    }

    private fun removeCard(unitType: UnitType) {
        val removed = draftDeck.remove(unitType)

        if (removed) {
            messageLabel.setText("${UnitRegistry.getConfig(unitType).displayName} removed.")
            messageLabel.color = UiTheme.statusInfo
        } else {
            messageLabel.setText("This card is not in the deck.")
            messageLabel.color = UiTheme.statusWarn
        }

        refreshDeckState()
    }

    private fun saveDeck() {
        if (draftDeck.size != MyGame.DECK_SIZE) {
            messageLabel.setText("Select exactly ${MyGame.DECK_SIZE} cards before saving.")
            messageLabel.color = UiTheme.statusWarn
            return
        }

        game.setSelectedDeck(draftDeck)
        draftDeck.clear()
        draftDeck += game.getSelectedDeck()
        messageLabel.setText("Deck saved. It will be used in the next match.")
        messageLabel.color = UiTheme.statusOk
        refreshDeckState()
    }

    private fun refreshDeckState() {
        UnitRegistry.all().forEach { config ->
            val copies = draftDeck.count { it == config.unitType }
            cardCountLabels[config.unitType]?.setText("In deck: $copies/${MyGame.MAX_CARD_COPIES}")
        }

        val names = draftDeck.joinToString(" · ") { UnitRegistry.getConfig(it).displayName }
            .ifBlank { "empty" }

        deckSummaryLabel.setText(
            "Current deck: ${draftDeck.size}/${MyGame.DECK_SIZE}\n$names"
        )
    }

    private fun roleTint(role: UnitRole): Color {
        return when (role) {
            UnitRole.CAPTURE -> Color(0.07f, 0.13f, 0.19f, 0.92f)
            UnitRole.ATTACK -> Color(0.17f, 0.09f, 0.12f, 0.92f)
            UnitRole.DEFENSE -> Color(0.11f, 0.12f, 0.18f, 0.92f)
            UnitRole.SUPPORT -> Color(0.08f, 0.14f, 0.13f, 0.92f)
            UnitRole.SPELL -> Color(0.14f, 0.10f, 0.19f, 0.92f)
        }
    }

    private fun formatRole(role: UnitRole): String {
        return when (role) {
            UnitRole.CAPTURE -> "Capture"
            UnitRole.ATTACK -> "Attack"
            UnitRole.DEFENSE -> "Defense"
            UnitRole.SUPPORT -> "Support"
            UnitRole.SPELL -> "Spell"
        }
    }
}
