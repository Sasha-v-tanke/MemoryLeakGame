package com.project.server.engine

import com.project.server.models.PlayerSession
import com.project.server.service.GameDispatcher
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.engine.config.GameConfig
import com.project.shared.engine.config.EntityConfig
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.config.WorldConfig
import com.project.shared.engine.commands.Command
import com.project.shared.engine.commands.PlayUnitCommand
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.components.*
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
        for (objectConfig in WorldConfig.objects) {
            createEntity(objectConfig)
        }

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

    fun createEntity(objectConfig: EntityConfig): Entity {
        val entity = world.createEntity()
        entity.add(Transform(objectConfig.x * GameConfig.worldWidth, objectConfig.y * GameConfig.worldHeight))
        entity.add(Owner(objectConfig.owner))
        entity.add(Sprite(objectConfig.sprite, objectConfig.scale))
        world.addEntity(entity)
        return entity
    }

    fun queueCommand(cmd: Command) {
        commandQueue.offer(cmd)
    }

    private fun processCommands() {
        while (true) {
            val cmd = commandQueue.poll() ?: break

            when (cmd) {
                is PlayUnitCommand -> {
                    val owner = if (cmd.playerId == players[0].playerId) {
                        OwnerType.PLAYER_1
                    } else {
                        OwnerType.PLAYER_2
                    }

                    UnitFactory.create(
                        world = world,
                        unitType = cmd.unitType,
                        owner = owner,
                        x = cmd.targetX,
                        y = cmd.targetY
                    )
                }

                else -> {
                    // todo
                }
            }
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
