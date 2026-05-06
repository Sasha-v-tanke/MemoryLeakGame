package com.project.client.ui.stages

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.widgets.DeckPanel
import com.project.shared.engine.PlayerResources
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitType

class UIStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    var onCardSelected: (UnitType) -> kotlin.Unit = {}

    private lateinit var topBar: Table
    private lateinit var bottomBar: Table

    private lateinit var hoverRoot: Table
    private lateinit var hoverPanel: Table
    private lateinit var hoverInfoLabel: Label

    private lateinit var selectedCardLabel: Label
    private lateinit var statusLabel: Label
    private lateinit var resourcesLabel: Label
    private lateinit var toastLabel: Label
    private lateinit var gameOverBox: Table

    override fun buildUI() {
        buildTopBar()
        buildBottomBar()
        buildHoverInfo()
        buildToast()
        buildGameOver()

        showToast("Waiting for opponent readiness...")
    }

    private fun buildTopBar() {
        topBar = Table()
        topBar.setFillParent(true)
        topBar.top().left()
        topBar.pad(10f)
        addActor(topBar)

        val panel = panel()

        statusLabel = Label(
            "Room: ${game.matchHandler.getRoomId().take(8)} · You: P${game.matchHandler.getPlayerIndex()} · Opponent: ${game.matchHandler.getOpponentId()}",
            skin
        )

        resourcesLabel = Label("Memory: - | CPU: -", skin)

        panel.add(statusLabel).left().padRight(18f)
        panel.add(resourcesLabel).left()

        topBar.add(panel).left()
    }

    private fun buildBottomBar() {
        bottomBar = Table()
        bottomBar.setFillParent(true)
        bottomBar.bottom()
        bottomBar.pad(12f)
        addActor(bottomBar)

        val panel = panel()
        panel.defaults().pad(4f)

        selectedCardLabel = Label("Selected: none · click card, then click arena", skin).apply {
            setAlignment(Align.center)
        }

        val deckPanel = DeckPanel(
            skin = skin,
            playerDeck = UnitRegistry.defaultDeck,
            onCardSelected = { unitType ->
                onCardSelected(unitType)
            }
        )

        panel.add(selectedCardLabel).growX().height(28f).row()
        panel.add(deckPanel).width(900f).height(128f)

        bottomBar.add(panel)
    }

    private fun buildHoverInfo() {
        hoverRoot = Table()
        hoverRoot.setFillParent(true)
        hoverRoot.top().right()
        hoverRoot.pad(10f)
        hoverRoot.isVisible = false

        hoverPanel = Table(skin).apply {
            background = skin.newDrawable(
                "default-round",
                Color(0.035f, 0.045f, 0.075f, 0.92f)
            )
            pad(10f)
        }

        hoverInfoLabel = Label("", skin).apply {
            setAlignment(Align.left)
            wrap = true
        }

        hoverPanel.add(hoverInfoLabel).width(320f)
        hoverRoot.add(hoverPanel).top().right()

        addActor(hoverRoot)
    }

    private fun buildToast() {
        toastLabel = Label("", skin).apply {
            setAlignment(Align.center)
            color = Color(0.75f, 0.95f, 1f, 1f)
            isVisible = false
        }

        val root = Table()
        root.setFillParent(true)
        root.top()
        root.padTop(78f)
        root.add(toastLabel).width(640f).height(34f)

        addActor(root)
    }

    private fun buildGameOver() {
        gameOverBox = Table()
        gameOverBox.setFillParent(true)
        gameOverBox.center()
        gameOverBox.isVisible = false
        addActor(gameOverBox)
    }

    fun startGame(message: String) {
        statusLabel.setText("Game started · Protect your Core · Destroy enemy Core")
        showToast(message)
    }

    fun updateResources(resources: PlayerResources) {
        resourcesLabel.setText(
            "Memory: ${resources.memory} (+${resources.memoryIncome}/s)  |  CPU: ${resources.cpu} (+${resources.cpuIncome}/s)"
        )
    }

    fun setSelectedCard(unitType: UnitType) {
        val config = UnitRegistry.getConfig(unitType)

        selectedCardLabel.setText(
            "Selected: ${config.displayName} · ${config.costMemory} Memory / ${config.costCpu} CPU · click arena to deploy"
        )

        showToast("${config.displayName}: ${config.gameDescription}")
    }

    fun clearSelectedCard() {
        selectedCardLabel.setText("Selected: none · click card, then click arena")
    }

    fun showHoverInfo(text: String) {
        hoverInfoLabel.setText(text)
        hoverRoot.isVisible = true
    }

    fun hideHoverInfo() {
        hoverRoot.isVisible = false
    }

    fun showToast(text: String) {
        toastLabel.clearActions()
        toastLabel.setText(text)
        toastLabel.color.a = 1f
        toastLabel.isVisible = true

        toastLabel.addAction(
            Actions.sequence(
                Actions.delay(2.25f),
                Actions.fadeOut(0.55f),
                Actions.run {
                    toastLabel.isVisible = false
                    toastLabel.color.a = 1f
                }
            )
        )
    }

    fun showGameOver(isWin: Boolean, reason: String) {
        gameOverBox.clearChildren()
        gameOverBox.isVisible = true

        val panel = Table(skin).apply {
            background = skin.newDrawable(
                "default-round",
                Color(0.025f, 0.035f, 0.055f, 0.96f)
            )
            pad(22f)
        }

        val title = Label(if (isWin) "SYSTEM ONLINE" else "CORE DUMPED", skin).apply {
            setAlignment(Align.center)
            fontScaleX = 1.45f
            fontScaleY = 1.45f
            color = if (isWin) Color(0.3f, 1f, 0.65f, 1f) else Color(1f, 0.35f, 0.35f, 1f)
        }

        val details = Label(
            if (isWin) {
                "Enemy Core destroyed. Your instance survived."
            } else {
                "Your Core was destroyed. System instance terminated."
            } + "\nReason: $reason",
            skin
        ).apply {
            setAlignment(Align.center)
            wrap = true
        }

        panel.add(title).width(520f).padBottom(14f).row()
        panel.add(details).width(520f).row()

        gameOverBox.add(panel)
    }
}
