package com.project.server.engine

import com.project.server.models.PlayerSession
import com.project.server.service.GameDispatcher
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.engine.GameConfig
import com.project.shared.engine.OwnerType
import com.project.shared.engine.commands.Command
import com.project.shared.engine.gameobjects.Entity
import com.project.shared.engine.gameobjects.components.*
import kotlinx.coroutines.*
import kotlin.collections.map

class GameRoom(
    private val roomId: String,
    private val players: List<PlayerSession>
) {
    private val world = GameWorld()
    private val commandQueue = java.util.concurrent.ConcurrentLinkedQueue<Command>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private var gameLoopJob: Job? = null
    private val playerStatus = PlayerStatusHandler(players.map { it.playerId })

    fun setPlayerReady(playerReady: PlayerReadyRequest) {
        playerStatus.setPlayerReady(playerReady)

        if (playerStatus.isAllReady()) {
            scope.launch {
                GameDispatcher.notifyPlayers(players)
            }
            startGame()
        }
    }

    fun startGame() {
        createEntity(OwnerType.PLAYER_1, GameConfig.worldWidth * 0.1f, GameConfig.worldHeight * 0.1f, "core.png")
        createEntity(OwnerType.PLAYER_2, GameConfig.worldWidth * 0.9f, GameConfig.worldHeight * 0.9f, "core.png")

        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob = scope.launch {
            val tickRate = 50L

            while (isActive) {
                val start = System.currentTimeMillis()

                processCommands()
//                updateSystems()
                sendSnapshot()

                val elapsed = System.currentTimeMillis() - start
                val delayTime = tickRate - elapsed
                if (delayTime > 0) delay(delayTime)
            }
        }
    }

    fun createEntity(owner: OwnerType, x: Float, y: Float, spriteId: String): Entity {
        val entity = world.createEntity()
        entity.add(Transform(x, y))
        entity.add(Owner(owner))
        entity.add(Sprite(spriteId))
        world.addEntity(entity)
        return entity
    }

    fun queueCommand(cmd: Command) {
        commandQueue.offer(cmd)
    }

    private fun processCommands() {
        while (true) {
            val cmd = commandQueue.poll() ?: break
        }
    }

    private suspend fun sendSnapshot() {
        val snapshot = GameStateSnapshotEvent(
            entities = world.getEntities().map { it.toState() },
            timestamp = System.currentTimeMillis()
        )

        GameDispatcher.sendToAllPlayers(players, snapshot)
    }

    fun stop() {
        gameLoopJob?.cancel()
    }
}
