package com.project.client.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.Vector2
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
import com.project.shared.engine.entities.components.CombatStats
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit
import com.project.shared.engine.entities.units.UnitType

class GameScreen(private val game: MyGame) : ScreenAdapter() {
    private val worldViewport = ExtendViewport(1280f, 720f)
    private val uiViewport = ScreenViewport()
    private val camera = GameCamera(game)
    private val worldStage = WorldStage(worldViewport, game)
    private val uiStage = UIStage(uiViewport, game)
    private val socket = GameSocket()

    private var gameStarted = false
    private var selectedCard: UnitType? = null
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
            selectedCard = unitType
            uiStage.setSelectedCard(unitType)
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
        val isWin = event.winnerPlayerId == game.getPlayerId()
        uiStage.showGameOver(isWin, event.reason)
        selectedCard = null
    }

    fun showSystemMessage(message: String) {
        uiStage.showToast(message)
    }

    private fun deploySelectedCard(worldX: Float, worldY: Float) {
        val card = selectedCard ?: return

        if (!gameStarted) {
            uiStage.showToast("Wait for game start")
            return
        }

        socket.playCard(
            playerId = game.getPlayerId(),
            roomId = game.matchHandler.getRoomId(),
            unitType = card,
            targetX = worldX,
            targetY = worldY
        ) { response ->
            if (response.success) {
                uiStage.showToast(response.description)
                selectedCard = null
                uiStage.clearSelectedCard()
            } else {
                uiStage.showToast(response.description.ifBlank { "Cannot play card" })
            }
        }
    }

    private fun onReadyResult(response: GameResponse) {
        println("Ready response: $response")
    }

    private fun updateHoverInfo() {
        val hovered = worldStage.getHoveredEntity(Gdx.input.x, Gdx.input.y)

        if (hovered == null) {
            uiStage.hideHoverInfo()
            return
        }

        val transform = hovered.components.filterIsInstance<Transform>().firstOrNull()
        val sprite = hovered.components.filterIsInstance<Sprite>().firstOrNull()
        val health = hovered.components.filterIsInstance<Health>().firstOrNull()
        val unit = hovered.components.filterIsInstance<Unit>().firstOrNull()
        val combat = hovered.components.filterIsInstance<CombatStats>().firstOrNull()
        val core = hovered.components.filterIsInstance<Core>().firstOrNull()
        val factory = hovered.components.filterIsInstance<Factory>().firstOrNull()
        val node = hovered.components.filterIsInstance<ResourceNode>().firstOrNull()

        uiStage.showHoverInfo(
            buildString {
                appendLine("Entity #${hovered.id}")
                appendLine("Owner: ${hovered.owner}")

                if (core != null) {
                    appendLine("Type: Core")
                    appendLine("Player Index: ${core.playerIndex}")
                }

                if (factory != null) {
                    appendLine("Type: ${factory.factoryType} Factory")
                    appendLine("Production: x${"%.1f".format(factory.productionMultiplier)}")
                }

                if (node != null) {
                    appendLine("Node: ${node.nodeType}")
                    appendLine("Captured by: ${node.capturedBy ?: "neutral"}")
                    appendLine("P1 progress: ${"%.0f".format(node.captureProgressPlayer1 * 100)}%")
                    appendLine("P2 progress: ${"%.0f".format(node.captureProgressPlayer2 * 100)}%")
                }

                if (unit != null) {
                    appendLine("Unit: ${unit.type}")
                    appendLine("Role: ${unit.role}")
                    appendLine("Cost: ${unit.costMemory} Memory / ${unit.costCpu} CPU")
                }

                if (health != null) {
                    appendLine("HP: ${health.current}/${health.max}")
                }

                if (combat != null) {
                    appendLine("DMG: ${combat.damage}")
                    appendLine("Range: ${combat.attackRange.toInt()}")
                    appendLine("Speed: ${combat.moveSpeed.toInt()}")
                }

                if (transform != null) {
                    appendLine("Pos: ${"%.0f".format(transform.x)}, ${"%.0f".format(transform.y)}")
                }

                if (sprite != null) {
                    appendLine("Sprite: ${sprite.textureId}")
                }
            }
        )
    }
}
