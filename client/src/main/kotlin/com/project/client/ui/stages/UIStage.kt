package com.project.client.ui.stages

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Dialog
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.theme.UiTheme
import com.project.client.ui.widgets.DeckPanel
import com.project.shared.engine.MatchStats
import com.project.shared.engine.PlayerResources
import com.project.shared.engine.entities.components.FactoryType
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitType

class UIStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    var onCardSelected: (UnitType) -> kotlin.Unit = {}
    var onBuildFactory: (FactoryType) -> kotlin.Unit = {}
    var onForfeitConfirmed: () -> kotlin.Unit = {}
    var onExitAfterGame: () -> kotlin.Unit = {}

    private lateinit var statusLabel: Label
    private lateinit var objectiveLabel: Label
    private lateinit var resourcesLabel: Label
    private lateinit var selectedCardLabel: Label
    private lateinit var hoverRoot: Table
    private lateinit var hoverInfoLabel: Label
    private lateinit var toastLabel: Label
    private lateinit var toastPanel: Table
    private lateinit var gameOverBox: Table
    private lateinit var deckPanel: DeckPanel
    private lateinit var statsBox: Table

    override fun buildUI() {
        buildTopBar()
        buildBottomBar()
        buildHoverInfo()
        buildToast()
        buildGameOver()
        showToast("Ожиданием противника...")
    }

    private fun buildTopBar() {
        val topBar = Table()
        topBar.setFillParent(true)
        topBar.top().left()
        topBar.pad(12f)
        addActor(topBar)

        val panel = panel(Color(0.03f, 0.06f, 0.12f, 0.90f), 12f)

        statusLabel = Label(
            "Матч: ${game.matchHandler.getRoomId().take(8)}  ·  Вы: P${game.matchHandler.getPlayerIndex()} · Оппонент: ${game.matchHandler.getOpponentId()}",
            skin
        ).apply { color = Color(0.84f, 0.92f, 1f, 1f) }

        objectiveLabel = Label("Захватывай память и CPU, расширяй фабрики, уничтожь ядро противника", skin).apply {
            color = Color(0.68f, 0.84f, 1f, 1f)
        }

        resourcesLabel = Label("Память: - | CPU: -", skin).apply {
            color = UiTheme.statusOk
        }

        val buildBasicButton = TextButton("Построить Basic Factory", skin)
        val buildSupportButton = TextButton("Построить Support Factory", skin)
        val forfeitButton = TextButton("Сдаться", skin)

        UiTheme.styleSecondaryButton(buildBasicButton, compact = true)
        UiTheme.styleSecondaryButton(buildSupportButton, compact = true)
        UiTheme.styleDangerButton(forfeitButton, compact = true)

        buildBasicButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                onBuildFactory(FactoryType.BASIC)
                true
            } else false
        }

        buildSupportButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                onBuildFactory(FactoryType.SUPPORT)
                true
            } else false
        }

        forfeitButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                showForfeitConfirm()
                true
            } else false
        }

        val buttons = Table(skin)
        buttons.defaults().padRight(6f)
        buttons.add(buildBasicButton).height(34f).width(190f)
        buttons.add(buildSupportButton).height(34f).width(205f)
        buttons.add(forfeitButton).height(34f).width(110f)

        panel.add(statusLabel).left().padRight(18f).padBottom(2f).row()
        panel.add(objectiveLabel).left().padBottom(6f).row()
        panel.add(resourcesLabel).left().padBottom(6f).row()
        panel.add(buttons).left()

        topBar.add(panel).left()
    }

    private fun buildBottomBar() {
        val bottomBar = Table()
        bottomBar.setFillParent(true)
        bottomBar.bottom()
        bottomBar.pad(12f)
        addActor(bottomBar)

        val panel = panel(Color(0.03f, 0.05f, 0.10f, 0.92f), 12f)
        panel.defaults().pad(4f)

        selectedCardLabel = Label("Выбрано: none  ·  выбери карту, потом нажми на арену", skin).apply {
            setAlignment(Align.center)
            color = Color(0.85f, 0.92f, 1f, 1f)
        }

        deckPanel = DeckPanel(
            skin = skin,
            playerDeck = game.getSelectedDeck(),
            onCardSelected = { unitType -> onCardSelected(unitType) }
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

        val hoverPanel = Table(skin).apply {
            background = skin.newDrawable("default-round", Color(0.030f, 0.045f, 0.080f, 0.94f))
            pad(12f)
        }

        hoverInfoLabel = Label("", skin).apply {
            setAlignment(Align.left)
            wrap = true
            color = Color(0.86f, 0.94f, 1f, 1f)
        }

        hoverPanel.add(hoverInfoLabel).width(380f)
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
            add(toastLabel).width(760f).height(28f).center()
        }

        val root = Table()
        root.setFillParent(true)
        root.top()
        root.padTop(82f)
        root.add(toastPanel).width(800f).height(44f)
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
        statusLabel.setText("Игра началась · Защити Ядро · Уничтожь Ядро противника")
        objectiveLabel.setText("Allocator захватывает память · GC освобождает память от потерянных юнитов · фабрики ускоряют производство")
        showToast(message)
    }

    fun updateResources(resources: PlayerResources) {
        resourcesLabel.color = when {
            resources.memory <= 2 || resources.cpu <= 1 -> UiTheme.statusError
            resources.memory <= 6 || resources.cpu <= 3 -> UiTheme.statusWarn
            else -> UiTheme.statusOk
        }
        resourcesLabel.setText(
            "Память: ${resources.memory} | CPU: ${resources.cpu} (+${resources.cpuIncome}/s) | Захвачено: ${resources.memoryAllocatedTotal} | Освобождено: ${
                resources
                    .memoryFreedTotal
            } |" +
                    " Фабрики: ${resources.factoriesBuilt}"
        )
    }

    fun updateCardRuntime(cooldownByCardMs: Map<UnitType, Long>, queueSizesByFactory: Map<FactoryType, Int>) {
        deckPanel.updateRuntime(cooldownByCardMs, queueSizesByFactory)
    }

    fun setSelectedCard(unitType: UnitType) {
        val config = UnitRegistry.getConfig(unitType)
        selectedCardLabel.setText("Selected: ${config.displayName} · ${config.costMemory} Memory / ${config.costCpu} CPU")
        deckPanel.setSelectedCard(unitType)
        showToast("${config.displayName}: ${config.realFeature}")
    }

    fun clearSelectedCard() {
        selectedCardLabel.setText("Выбрано: none  ·  выбери карту, потом нажми на арену")
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

    fun showGameOver(isWin: Boolean, reason: String, stats: MatchStats) {
        gameOverBox.clearChildren()
        gameOverBox.isVisible = true

        val panel = Table(skin).apply {
            background = skin.newDrawable("default-round", Color(0.02f, 0.04f, 0.07f, 0.97f))
            pad(24f)
        }

        val title = Label(if (isWin) "Противник уничтожен" else "Ядро уничтожено", skin).apply {
            setAlignment(Align.center)
            color = if (isWin) Color(0.34f, 1f, 0.72f, 1f) else Color(1f, 0.40f, 0.40f, 1f)
        }

        val details = Label(
            (if (isWin) "Ядро противника уничтожено. Твое Ядро выжило." else "Твоя Ядро уничтожено. Процесс терминирован.") +
                    "\nReason: $reason",
            skin
        ).apply {
            setAlignment(Align.center)
            wrap = true
        }

        statsBox = Table(skin)
        statsBox.defaults().pad(4f).left()
        fillStats(stats)

        val scroll = ScrollPane(statsBox, skin)
        scroll.setFadeScrollBars(false)

        val exitButton = TextButton("Выход в меню", skin)
        UiTheme.stylePrimaryButton(exitButton)
        exitButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                onExitAfterGame()
                true
            } else false
        }

        panel.add(title).width(680f).padBottom(10f).row()
        panel.add(details).width(680f).padBottom(14f).row()
        panel.add(scroll).width(760f).height(280f).padBottom(14f).row()
        panel.add(exitButton).width(260f).height(44f).row()
        gameOverBox.add(panel)
    }

    private fun fillStats(stats: MatchStats) {
        val myId = game.getPlayerId()
        val myStats = stats.byPlayerId[myId]
        val enemyStats = stats.byPlayerId.entries.firstOrNull { it.key != myId }?.value

        statsBox.add(Label("Match statistics", skin, "title")).colspan(2).padBottom(8f).row()
        statsBox.add(Label("Your system", skin)).width(360f)
        statsBox.add(Label("Enemy system", skin)).width(360f).row()
        statsBox.add(Label(formatStats(myStats), skin, "small")).width(360f).top()
        statsBox.add(Label(formatStats(enemyStats), skin, "small")).width(360f).top().row()
    }

    private fun formatStats(stats: com.project.shared.engine.PlayerMatchStats?): String {
        if (stats == null) return "Нет информации"
        return buildString {
            appendLine("Очередь: ${stats.unitsQueued}")
            appendLine("Произведено: ${stats.unitsProduced}")
            appendLine("Потеряно: ${stats.unitsLost}")
            appendLine("Убито: ${stats.enemyUnitsKilled}")
            appendLine("Памяти выделено: ${stats.memoryAllocated}")
            appendLine("Памяти освобождено: ${stats.memoryFreed}")
            appendLine("Фабрики: ${stats.factoriesBuilt}")
            appendLine("Магия: ${stats.spellsCast}")
        }
    }

    private fun showForfeitConfirm() {
        val dialog = object : Dialog("Принять поражение?", skin) {
            override fun result(obj: Any?) {
                if (obj == true) {
                    onForfeitConfirmed()
                }
            }
        }

        dialog.text("Leaving now counts as automatic defeat.\nConfirm termination of your instance?")
        dialog.button("Cancel", false)
        dialog.button("Forfeit", true)
        dialog.show(this)
    }

}
