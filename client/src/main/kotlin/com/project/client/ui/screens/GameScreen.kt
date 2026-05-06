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
    private var lastSnapshot: GameStateSnapshotEvent? = null

    override fun show() {
        worldStage.buildUI()
        uiStage.buildUI()

        worldViewport.camera = camera
        camera.initialize()

        worldStage.onWorldClicked = { x, y ->
            deploySelectedCard(x, y)
        }

        uiStage.onCardSelected = { unitType ->
            if (gameFinished) {
                uiStage.showToast("Match is finished")
            } else if (isDeployingCard) {
                uiStage.showToast("Wait for current deployment")
            } else {
                selectedCard = unitType
                uiStage.setSelectedCard(unitType)
            }
        }

        Gdx.input.inputProcessor = InputMultiplexer(uiStage, worldStage)

        worldStage.show()
        uiStage.show()

        socket.sendPlayerReady(
            playerId = game.getPlayerId(),
            roomId = game.matchHandler.getRoomId()
        ) { response ->
            onReadyResult(response)
        }
    }

    override fun render(delta: Float) {
        camera.update(delta)

        Gdx.gl.glClearColor(0.012f, 0.014f, 0.024f, 1f)
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
        uiStage.showToast("Game started. Select a card, then click the arena.")
    }

    fun updateGameState(snapshotEvent: GameStateSnapshotEvent) {
        lastSnapshot = snapshotEvent

        worldStage.applySnapshot(snapshotEvent)

        val resources = snapshotEvent.resources[game.getPlayerId()]
        if (resources != null) {
            uiStage.updateResources(resources)
        }
    }

    fun finishGame(event: GameOverEvent) {
        gameFinished = true
        gameStarted = false
        isDeployingCard = false

        val isWin = event.winnerPlayerId == game.getPlayerId()
        uiStage.showGameOver(isWin, event.reason)

        selectedCard = null
        uiStage.clearSelectedCard()

        println("[CLIENT][GAME_OVER] winner=${event.winnerPlayerId} loser=${event.loserPlayerId} reason=${event.reason}")
    }

    fun showSystemMessage(message: String) {
        uiStage.showToast(message)
        println("[CLIENT][SYSTEM] $message")
    }

    private fun deploySelectedCard(worldX: Float, worldY: Float) {
        if (gameFinished) {
            uiStage.showToast("Match is finished")
            return
        }

        if (!gameStarted) {
            uiStage.showToast("Wait: game starts when both players are ready")
            return
        }

        if (isDeployingCard) {
            uiStage.showToast("Deployment in progress")
            return
        }

        val card = selectedCard

        if (card == null) {
            uiStage.showToast("Select a card first")
            return
        }

        val config = UnitRegistry.getConfig(card)

        isDeployingCard = true
        selectedCard = null
        uiStage.clearSelectedCard()

        println("[CLIENT][CARD] sending card=$card x=$worldX y=$worldY")
        uiStage.showToast("Deploying ${config.displayName}...")

        socket.playCard(
            playerId = game.getPlayerId(),
            roomId = game.matchHandler.getRoomId(),
            unitType = card,
            targetX = worldX,
            targetY = worldY
        ) { response ->
            isDeployingCard = false

            println("[CLIENT][CARD] response=$response")

            if (response.success) {
                uiStage.showToast(response.description)
            } else {
                selectedCard = card
                uiStage.setSelectedCard(card)
                uiStage.showToast(response.description.ifBlank { "Cannot play card" })
            }
        }
    }

    private fun onReadyResult(response: GameResponse) {
        println("[CLIENT][READY] $response")
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

        val text = when {
            unit != null -> buildUnitHoverText(
                unit = unit,
                combat = combat,
                health = health,
                owner = hovered.owner
            )

            core != null -> buildCoreHoverText(
                health = health,
                owner = hovered.owner
            )

            factory != null -> buildFactoryHoverText(
                factory = factory,
                health = health,
                owner = hovered.owner
            )

            node != null -> buildNodeHoverText(node)

            else -> "Unknown object"
        }

        uiStage.showHoverInfo(text)
    }

    private fun buildUnitHoverText(
        unit: Unit,
        combat: CombatStats?,
        health: Health?,
        owner: OwnerType
    ): String {
        val config = UnitRegistry.getConfig(unit.typeName)

        return buildString {
            appendLine(config.displayName)
            appendLine("${formatOwner(owner)} · ${formatRole(config.role)}")

            if (health != null) {
                appendLine("HP: ${health.current}/${health.max}")
            }

            if (combat != null && config.role != UnitRole.SPELL) {
                appendLine("Damage: ${combat.damage}")
                appendLine("Range: ${combat.attackRange.toInt()}")
                appendLine("Speed: ${combat.moveSpeed.toInt()}")
            }

            appendLine("Cost: ${config.costMemory} Memory / ${config.costCpu} CPU")
            appendLine()
            appendLine(config.gameDescription)
            appendLine()
            appendLine("IT: ${config.techDescription}")
        }
    }

    private fun buildCoreHoverText(
        health: Health?,
        owner: OwnerType
    ): String {
        return buildString {
            appendLine("Core")
            appendLine(formatOwner(owner))

            if (health != null) {
                appendLine("HP: ${health.current}/${health.max}")
            }

            appendLine()
            appendLine("Main static objective of the instance.")
            appendLine("Destroy the enemy Core to win the match.")
            appendLine()
            appendLine("IT: represents the central runtime/kernel of a digital system.")
        }
    }

    private fun buildFactoryHoverText(
        factory: Factory,
        health: Health?,
        owner: OwnerType
    ): String {
        val title = when (factory.factoryType) {
            FactoryType.BASIC -> "Basic Factory"
            FactoryType.SUPPORT -> "Support Factory"
        }

        val description = when (factory.factoryType) {
            FactoryType.BASIC -> "Base production infrastructure for combat units."
            FactoryType.SUPPORT -> "Infrastructure focused on support and advanced system tools."
        }

        val tech = when (factory.factoryType) {
            FactoryType.BASIC -> "IT: a basic build pipeline that produces system processes."
            FactoryType.SUPPORT -> "IT: auxiliary services that keep the system stable and extensible."
        }

        return buildString {
            appendLine(title)
            appendLine(formatOwner(owner))

            if (health != null) {
                appendLine("HP: ${health.current}/${health.max}")
            }

            appendLine("Production: x${"%.1f".format(factory.productionMultiplier)}")
            appendLine()
            appendLine(description)
            appendLine()
            appendLine(tech)
        }
    }

    private fun buildNodeHoverText(node: ResourceNode): String {
        val title = when (node.nodeType) {
            ResourceNodeType.CPU -> "CPU Node"
            ResourceNodeType.MEMORY -> "Memory Node"
        }

        val ownerText = when (node.capturedBy) {
            1 -> "Controlled by Player 1"
            2 -> "Controlled by Player 2"
            else -> "Neutral"
        }

        val progressText = when {
            node.captureProgressPlayer1 > node.captureProgressPlayer2 ->
                "Capture: Player 1 ${(node.captureProgressPlayer1 * 100).toInt()}%"

            node.captureProgressPlayer2 > node.captureProgressPlayer1 ->
                "Capture: Player 2 ${(node.captureProgressPlayer2 * 100).toInt()}%"

            else -> "Capture: 0%"
        }

        val gameplay = when (node.nodeType) {
            ResourceNodeType.CPU -> "Gives CPU income. CPU helps deploy stronger cards and maintain tempo."
            ResourceNodeType.MEMORY -> "Gives Memory income. Memory is the main card deployment resource."
        }

        val tech = when (node.nodeType) {
            ResourceNodeType.CPU -> "IT: CPU represents compute throughput and execution capacity."
            ResourceNodeType.MEMORY -> "IT: Memory represents available working space for active processes."
        }

        return buildString {
            appendLine(title)
            appendLine(ownerText)
            appendLine(progressText)
            appendLine("Income: +${node.incomePerSecond}/s")
            appendLine()
            appendLine(gameplay)
            appendLine()
            appendLine(tech)
        }
    }

    private fun formatOwner(owner: OwnerType): String {
        return when (owner) {
            OwnerType.PLAYER_1 -> "Player 1"
            OwnerType.PLAYER_2 -> "Player 2"
            OwnerType.WORLD -> "Neutral"
        }
    }

    private fun formatRole(role: UnitRole): String {
        return when (role) {
            UnitRole.CAPTURE -> "Capturer"
            UnitRole.SUPPORT -> "Support"
            UnitRole.DEFENSE -> "Defender"
            UnitRole.ATTACK -> "Attacker"
            UnitRole.SPELL -> "Spell"
        }
    }
}
