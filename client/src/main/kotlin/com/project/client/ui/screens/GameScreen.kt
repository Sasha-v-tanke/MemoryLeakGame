package com.project.client.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.project.client.MyGame
import com.project.client.network.api.GameSocket
import com.project.client.ui.managers.GameCamera
import com.project.client.ui.stages.UIStage
import com.project.client.ui.stages.WorldStage
import com.project.client.ui.theme.UiTheme
import com.project.shared.api.events.GameOverEvent
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.game.GameResponse
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.CombatStats
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.FactoryType
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.ProcessPhase
import com.project.shared.engine.entities.components.ProcessState
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.ResourceNodeType
import com.project.shared.engine.entities.components.Unit
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitRole
import com.project.shared.engine.entities.units.UnitType

class GameScreen(private val game: MyGame) : ScreenAdapter() {
    private val worldViewport = ExtendViewport(1280f, 720f)
    private val uiViewport = ScreenViewport()
    private val camera = GameCamera(game)
    private val worldStage = WorldStage(worldViewport, game)
    private val uiStage = UIStage(uiViewport, game)
    private val socket = GameSocket()

    private var gameStarted = false
    private var gameFinished = false
    private var selectedCard: UnitType? = null
    private var isDeployingCard = false

    override fun show() {
        worldStage.buildUI()
        uiStage.buildUI()

        worldViewport.camera = camera
        camera.initialize()

        worldStage.onWorldClicked = { x, y -> deploySelectedCard(x, y) }

        uiStage.onCardSelected = { unitType ->
            if (gameFinished) uiStage.showToast("Матч завершён")
            else if (isDeployingCard) uiStage.showToast("Подождите: текущая отправка карты в процессе")
            else {
                selectedCard = unitType
                uiStage.setSelectedCard(unitType)
            }
        }

        uiStage.onBuildFactory = { factoryType ->
            socket.buildFactory(game.getPlayerId(), game.matchHandler.getRoomId(), factoryType) { response ->
                uiStage.showToast(response.description)
            }
        }

        uiStage.onForfeitConfirmed = {
            socket.forfeit(game.getPlayerId(), game.matchHandler.getRoomId()) { response ->
                uiStage.showToast(response.description)
            }
        }

        uiStage.onExitAfterGame = {
            socket.close()
            game.returnToMainMenu()
        }

        Gdx.input.inputProcessor = InputMultiplexer(uiStage, worldStage)

        worldStage.show()
        uiStage.show()

        socket.sendPlayerReady(game.getPlayerId(), game.matchHandler.getRoomId()) { response -> onReadyResult(response) }
    }

    override fun render(delta: Float) {
        camera.update(delta)
        Gdx.gl.glClearColor(UiTheme.background.r, UiTheme.background.g, UiTheme.background.b, UiTheme.background.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        worldStage.act(delta)
        worldStage.draw()
        uiStage.act(delta)
        uiStage.draw()
        updateHoverInfo()
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width, height, false)
        uiViewport.update(width, height, true)
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
        socket.close()
    }

    override fun dispose() {
        socket.close()
        worldStage.dispose()
        uiStage.dispose()
    }

    fun startGame(event: GameStartEvent) {
        gameStarted = true
        uiStage.startGame(event.message)
        uiStage.showToast("Allocator создаёт Memory. Garbage Collector освобождает мёртвые выделения.")
    }

    fun updateGameState(snapshotEvent: GameStateSnapshotEvent) {
        worldStage.applySnapshot(snapshotEvent)
        worldStage.applyTextEvents(snapshotEvent.textEvents)

        snapshotEvent.resources[game.getPlayerId()]?.let { uiStage.updateResources(it) }

        val cooldowns = snapshotEvent.cardCooldownsMs[game.getPlayerId()].orEmpty()
        val queues = snapshotEvent.factoryQueueSizes[game.getPlayerId()].orEmpty()
        uiStage.updateCardRuntime(cooldowns, queues)
    }

    fun finishGame(event: GameOverEvent) {
        gameFinished = true
        gameStarted = false
        isDeployingCard = false
        selectedCard = null
        uiStage.clearSelectedCard()
        uiStage.showGameOver(event.winnerPlayerId == game.getPlayerId(), event.reason, event.stats)
    }

    fun showSystemMessage(message: String) {
        uiStage.showToast(message)
    }

    private fun deploySelectedCard(worldX: Float, worldY: Float) {
        if (gameFinished) {
            uiStage.showToast("Матч завершён")
            return
        }

        if (!gameStarted) {
            uiStage.showToast("Подождите: игра начнётся, когда оба игрока будут готовы")
            return
        }

        if (isDeployingCard) {
            uiStage.showToast("Развёртывание в процессе")
            return
        }

        val card = selectedCard ?: run {
            uiStage.showToast("Сначала выберите карту")
            return
        }

        val config = UnitRegistry.getConfig(card)
        val isManualTargetCard = config.role == UnitRole.SPELL

        isDeployingCard = true
        selectedCard = null
        uiStage.clearSelectedCard()

        if (isManualTargetCard) {
            uiStage.showToast("Применение ${config.displayName} в выбранной точке...")
        } else {
            uiStage.showToast("Планирование ${config.displayName}. Цель будет выбрана автоматически.")
        }

        socket.playCard(game.getPlayerId(), game.matchHandler.getRoomId(), card, worldX, worldY) { response ->
            isDeployingCard = false
                if (response.success) {
                    uiStage.showToast(response.description)
                } else {
                    selectedCard = card
                    uiStage.setSelectedCard(card)
                    uiStage.showToast(response.description.ifBlank { "Невозможно разыграть карту" })
                }
        }
    }

    private fun onReadyResult(response: GameResponse) {
        Gdx.app.log("GameScreen", "Ready response=$response")
    }

    private fun updateHoverInfo() {
        val hovered = worldStage.getHoveredEntity(Gdx.input.x, Gdx.input.y)

        if (hovered == null) {
            uiStage.hideHoverInfo()
            return
        }

        val health = hovered.components.filterIsInstance<Health>().firstOrNull()
        val unit = hovered.components.filterIsInstance<Unit>().firstOrNull()
        val combat = hovered.components.filterIsInstance<CombatStats>().firstOrNull()
        val core = hovered.components.filterIsInstance<Core>().firstOrNull()
        val factory = hovered.components.filterIsInstance<Factory>().firstOrNull()
        val node = hovered.components.filterIsInstance<ResourceNode>().firstOrNull()
        val process = hovered.components.filterIsInstance<ProcessState>().firstOrNull()

        val text = when {
            unit != null -> buildUnitHoverText(unit, combat, health, process, hovered.owner)
            core != null -> buildCoreHoverText(health, hovered.owner)
            factory != null -> buildFactoryHoverText(factory, health, hovered.owner)
            node != null -> buildNodeHoverText(node)
            else -> "Неизвестный объект"
        }

        uiStage.showHoverInfo(text)
    }

    private fun buildUnitHoverText(unit: Unit, combat: CombatStats?, health: Health?, process: ProcessState?, owner: OwnerType): String {
        val config = UnitRegistry.getConfig(unit.typeName)

        return buildString {
            appendLine(config.displayName)
            appendLine("${formatOwner(owner)} · ${formatRole(config.role)}")
            appendLine("Удержано Memory: ${unit.allocatedMemory}")
            if (health != null) appendLine("HP: ${health.current}/${health.max}")
            if (combat != null && config.role != UnitRole.SPELL && combat.damage > 0) {
                appendLine("Урон: ${combat.damage} · Дальность: ${combat.attackRange.toInt()} · Скорость: ${combat.moveSpeed.toInt()}")
            }
            if (process != null) {
                appendLine("Состояние: ${formatPhase(process.phase)}")
                if (process.lastEvent.isNotBlank()) appendLine("Событие: ${process.lastEvent}")
            }
            appendLine()
            appendLine(config.gameDescription)
            appendLine("Сильные стороны: ${config.strengths}")
            appendLine("Слабые стороны: ${config.weaknesses}")
            appendLine()
            appendLine("IT: ${config.realFeature}")
        }
    }

    private fun buildCoreHoverText(health: Health?, owner: OwnerType): String {
        return buildString {
            appendLine("Ядро")
            appendLine(formatOwner(owner))
            if (health != null) appendLine("HP: ${health.current}/${health.max}")
            appendLine()
            appendLine("Уничтожение Ядра завершает экземпляр системы.")
            appendLine("IT: сбой центрального рантайма/ядра останавливает систему.")
        }
    }

    private fun buildFactoryHoverText(factory: Factory, health: Health?, owner: OwnerType): String {
        val title = when (factory.factoryType) {
            FactoryType.BASIC -> "Basic Factory"
            FactoryType.SUPPORT -> "Support Factory"
        }

        return buildString {
            appendLine(title)
            appendLine(formatOwner(owner))
            if (health != null) appendLine("HP: ${health.current}/${health.max}")
            appendLine("Множитель производства: x${"%.2f".format(factory.productionMultiplier)}")
            appendLine()
            appendLine("Постройте больше фабрик, чтобы увеличить параллельное производство и ёмкость очереди.")
            appendLine("IT: масштабирование конвейеров сборки увеличивает пропускную способность, но потребляет Memory и CPU.")
        }
    }

    private fun buildNodeHoverText(node: ResourceNode): String {
        val title = when (node.nodeType) {
            ResourceNodeType.CPU -> "CPU-узел"
            ResourceNodeType.MEMORY -> "Источник Memory"
        }

        return buildString {
            appendLine(title)
            appendLine(
                when (node.capturedBy) {
                    1 -> "Контролируется Игроком 1"
                    2 -> "Контролируется Игроком 2"
                    else -> "Нейтральный"
                }
            )
            appendLine()
            if (node.nodeType == ResourceNodeType.MEMORY) {
                appendLine("Allocator должен работать здесь, чтобы создать пригодные пачки Memory.")
                appendLine("IT: Memory существует как ёмкость, но процесс должен выделить её перед использованием.")
            } else {
                appendLine("Контролируемые CPU-узлы увеличивают поступление CPU.")
                appendLine("IT: пропускная способность CPU ограничивает количество операций, которые система может выполнять.")
            }
        }
    }

    private fun formatOwner(owner: OwnerType): String {
        return when (owner) {
            OwnerType.PLAYER_1 -> "Игрок 1"
            OwnerType.PLAYER_2 -> "Игрок 2"
            OwnerType.WORLD -> "Нейтральный"
        }
    }

    private fun formatRole(role: UnitRole): String {
        return when (role) {
            UnitRole.CAPTURE -> "Захват ресурсов"
            UnitRole.SUPPORT -> "Поддержка"
            UnitRole.DEFENSE -> "Защита"
            UnitRole.ATTACK -> "Атака"
            UnitRole.SPELL -> "Эффект"
        }
    }

    private fun formatPhase(phase: ProcessPhase): String {
        return when (phase) {
            ProcessPhase.RUNNING -> "выполняется"
            ProcessPhase.COMPLETED -> "завершён"
            ProcessPhase.DEAD -> "мертв (ожидание GC)"
            ProcessPhase.GARBAGE_COLLECTING -> "сборка мусора"
            ProcessPhase.WORKING -> "в работе"
        }
    }
}
