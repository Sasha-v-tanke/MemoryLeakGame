package com.project.client.ui.stages

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.theme.UiTheme
import com.project.client.ui.widgets.DeckPanel
import com.project.shared.engine.PlayerResources
import com.project.shared.engine.entities.components.FactoryType
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
    private lateinit var objectiveLabel: Label
    private lateinit var resourcesLabel: Label
    private lateinit var toastLabel: Label
    private lateinit var toastPanel: Table
    private lateinit var gameOverBox: Table
    private lateinit var deckPanel: DeckPanel

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
        topBar.pad(12f)
        addActor(topBar)

        val panel = panel(Color(0.03f, 0.06f, 0.12f, 0.90f), 12f)

        statusLabel = Label(
            "Room: ${game.matchHandler.getRoomId().take(8)} · You: P${game.matchHandler.getPlayerIndex()} · Opponent: ${game.matchHandler.getOpponentId()}",
            skin
        ).apply { color = Color(0.84f, 0.92f, 1f, 1f) }
        objectiveLabel = Label("Objective: Capture nodes, break enemy Core", skin).apply {
            color = Color(0.68f, 0.84f, 1f, 1f)
        }

        resourcesLabel = Label("Memory: - | CPU: -", skin)
        resourcesLabel.color = UiTheme.statusOk

        panel.add(statusLabel).left().padRight(18f).padBottom(2f).row()
        panel.add(objectiveLabel).left().padBottom(6f).row()
        panel.add(resourcesLabel).left()

        topBar.add(panel).left()
    }

    private fun buildBottomBar() {
        bottomBar = Table()
        bottomBar.setFillParent(true)
        bottomBar.bottom()
        bottomBar.pad(12f)
        addActor(bottomBar)

        val panel = panel(Color(0.03f, 0.05f, 0.10f, 0.92f), 12f)
        panel.defaults().pad(4f)

        selectedCardLabel = Label("Selected: none · click card, then click arena", skin).apply {
            setAlignment(Align.center)
            color = Color(0.85f, 0.92f, 1f, 1f)
        }

        deckPanel = DeckPanel(
            skin = skin,
            playerDeck = game.getSelectedDeck(),
            onCardSelected = { unitType ->
                onCardSelected(unitType)
            }
        )

        panel.add(selectedCardLabel).growX().height(30f).row()
        panel.add(deckPanel).width(940f).height(150f)

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
                Color(0.030f, 0.045f, 0.080f, 0.94f)
            )
            pad(12f)
        }

        hoverInfoLabel = Label("", skin).apply {
            setAlignment(Align.left)
            wrap = true
            color = Color(0.86f, 0.94f, 1f, 1f)
        }

        hoverPanel.add(hoverInfoLabel).width(350f)
        hoverRoot.add(hoverPanel).top().right()

        addActor(hoverRoot)
    }

    private fun buildToast() {
        toastLabel = Label("", skin).apply {
            setAlignment(Align.center)
            color = Color(0.82f, 0.94f, 1f, 1f)
            isVisible = false
        }
        toastPanel = panel(Color(0.04f, 0.09f, 0.15f, 0.92f), 8f).apply {
            isVisible = false
            add(toastLabel).width(720f).height(28f).center()
        }

        val root = Table()
        root.setFillParent(true)
        root.top()
        root.padTop(82f)
        root.add(toastPanel).width(760f).height(44f)

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
        objectiveLabel.setText("Objective: Control Memory/CPU tempo and finish the Core")
        showToast(message)
    }

    fun updateResources(resources: PlayerResources) {
        resourcesLabel.color = when {
            resources.memory <= 2 || resources.cpu <= 1 -> UiTheme.statusError
            resources.memory <= 6 || resources.cpu <= 3 -> UiTheme.statusWarn
            else -> UiTheme.statusOk
        }
        resourcesLabel.setText(
            "Memory: ${resources.memory} (+${resources.memoryIncome}/s)  |  CPU: ${resources.cpu} (+${resources.cpuIncome}/s)"
        )
    }

    fun updateCardRuntime(
        cooldownByCardMs: Map<UnitType, Long>,
        queueSizesByFactory: Map<FactoryType, Int>
    ) {
        deckPanel.updateRuntime(cooldownByCardMs, queueSizesByFactory)
    }

    fun setSelectedCard(unitType: UnitType) {
        val config = UnitRegistry.getConfig(unitType)

        selectedCardLabel.setText(
            "Selected: ${config.displayName} · ${config.costMemory} Memory / ${config.costCpu} CPU · click arena to deploy"
        )

        deckPanel.setSelectedCard(unitType)
        showToast("${config.displayName}: ${config.gameDescription}")
    }

    fun clearSelectedCard() {
        selectedCardLabel.setText("Selected: none · click card, then click arena")
        deckPanel.setSelectedCard(null)
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
        toastPanel.clearActions()
        toastLabel.setText(text)
        toastLabel.color.a = 1f
        toastLabel.isVisible = true
        toastPanel.color.a = 1f
        toastPanel.isVisible = true

        toastPanel.addAction(
            Actions.sequence(
                Actions.delay(2.25f),
                Actions.fadeOut(0.55f),
                Actions.run {
                    toastLabel.isVisible = false
                    toastPanel.isVisible = false
                    toastLabel.color.a = 1f
                    toastPanel.color.a = 1f
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
                Color(0.02f, 0.04f, 0.07f, 0.96f)
            )
            pad(24f)
        }

        val title = Label(if (isWin) "SYSTEM ONLINE" else "CORE DUMPED", skin).apply {
            setAlignment(Align.center)
            color = if (isWin) Color(0.34f, 1f, 0.72f, 1f) else Color(1f, 0.40f, 0.40f, 1f)
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
