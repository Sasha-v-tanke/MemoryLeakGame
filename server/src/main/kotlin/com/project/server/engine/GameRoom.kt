package com.project.server.engine

import com.project.server.models.PlayerRuntime
import com.project.server.models.PlayerSession
import com.project.server.service.GameDispatcher
import com.project.shared.api.events.GameOverEvent
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.events.SystemMessageEvent
import com.project.shared.api.game.PlayCardRequest
import com.project.shared.api.game.PlayCardResponse
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.engine.config.GameConfig
import com.project.shared.engine.config.WorldConfig
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.AttackBehavior
import com.project.shared.engine.entities.components.CaptureBehavior
import com.project.shared.engine.entities.components.CombatStats
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.Owner
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.ResourceNodeType
import com.project.shared.engine.entities.components.StatusEffects
import com.project.shared.engine.entities.components.SupportBehavior
import com.project.shared.engine.entities.components.Target
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitRole
import com.project.shared.engine.entities.units.UnitType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

class GameRoom(
    private val roomId: String,
    private val players: List<PlayerSession>,
    private val onFinished: (String) -> Unit
) {
    private val world = GameWorld()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val playerStatus = PlayerStatusHandler(players.map { it.playerId })
    private val started = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)

    private var gameLoopJob: Job? = null
    private var tick: Long = 0L
    private var lastIncomeAt: Long = System.currentTimeMillis()

    private val playerRuntimes: Map<Int, PlayerRuntime> = players.mapIndexed { index, session ->
        session.playerId to PlayerRuntime(
            playerId = session.playerId,
            playerIndex = index + 1
        )
    }.toMap()

    fun setPlayerReady(playerReady: PlayerReadyRequest) {
        if (finished.get()) return

        playerStatus.setPlayerReady(playerReady)

        if (playerStatus.isAllReady() && started.compareAndSet(false, true)) {
            scope.launch {
                startGame()
            }
        }
    }

    fun playCard(request: PlayCardRequest): PlayCardResponse {
        if (!started.get()) {
            return PlayCardResponse(false, "Game is not started yet")
        }

        if (finished.get()) {
            return PlayCardResponse(false, "Game is already finished")
        }

        val runtime = playerRuntimes[request.playerId]
            ?: return PlayCardResponse(false, "Player does not belong to this room")

        val config = UnitRegistry.getConfig(request.unitType)

        if (runtime.memory < config.costMemory) {
            return PlayCardResponse(false, "Not enough Memory")
        }

        if (runtime.cpu < config.costCpu) {
            return PlayCardResponse(false, "Not enough CPU")
        }

        runtime.memory -= config.costMemory
        runtime.cpu -= config.costCpu

        val owner = OwnerType.fromPlayerIndex(runtime.playerIndex)
        val targetX = request.targetX.coerceIn(0f, GameConfig.worldWidth)
        val targetY = request.targetY.coerceIn(0f, GameConfig.worldHeight)

        when (request.unitType) {
            UnitType.DEADLOCK -> castDeadlock(owner, targetX, targetY)
            UnitType.OVERCLOCK -> castOverclock(owner, targetX, targetY)
            else -> UnitFactory.createUnit(world, request.unitType, owner, targetX, targetY)
        }

        return PlayCardResponse(true, "${config.displayName} deployed")
    }

    private suspend fun startGame() {
        buildWorld()
        GameDispatcher.notifyPlayersGameStarted(players)
        startGameLoop()
    }

    private fun buildWorld() {
        WorldConfig.objects.forEach { config ->
            EntityFactory.createWorldObject(world, config)
        }
    }

    private fun startGameLoop() {
        gameLoopJob = scope.launch {
            while (isActive && !finished.get()) {
                val start = System.currentTimeMillis()

                update(GameConfig.tickMillis / 1000f)
                if (tick % GameConfig.snapshotEveryTicks == 0L) {
                    sendSnapshot()
                }

                tick++

                val elapsed = System.currentTimeMillis() - start
                val delayTime = GameConfig.tickMillis - elapsed
                if (delayTime > 0) delay(delayTime)
            }
        }
    }

    private suspend fun sendSnapshot() {
        val snapshot = GameStateSnapshotEvent(
            entities = world.getEntities().map { it.toState() },
            resources = playerRuntimes.values.associate { it.playerId to it.toResources() },
            timestamp = System.currentTimeMillis(),
            tick = tick
        )

        GameDispatcher.sendToAllPlayers(players, snapshot)
    }

    private fun update(deltaSeconds: Float) {
        val now = System.currentTimeMillis()

        updateResourceIncome(now)
        updateNodeCapture(deltaSeconds)
        updateUnitTargets()
        updateMovement(deltaSeconds, now)
        updateCombat(now)
        updateSupport(now)
        updateCoreDeath()

        world.removeDeadNonCoreEntities()
    }

    private fun updateResourceIncome(now: Long) {
        if (now - lastIncomeAt < GameConfig.resourceIncomeIntervalMillis) return
        lastIncomeAt = now

        recalculateIncome()

        playerRuntimes.values.forEach { runtime ->
            runtime.memory += runtime.memoryIncome
            runtime.cpu += runtime.cpuIncome
        }
    }

    private fun recalculateIncome() {
        playerRuntimes.values.forEach { runtime ->
            runtime.memoryIncome = GameConfig.baseMemoryIncome
            runtime.cpuIncome = GameConfig.baseCpuIncome
        }

        world.entitiesWithComponent(ResourceNode::class.java).forEach { entity ->
            val node = entity.get(ResourceNode::class.java) ?: return@forEach
            val capturedBy = node.capturedBy ?: return@forEach
            val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == capturedBy } ?: return@forEach

            when (node.nodeType) {
                ResourceNodeType.MEMORY -> runtime.memoryIncome += node.incomePerSecond
                ResourceNodeType.CPU -> runtime.cpuIncome += node.incomePerSecond
            }
        }
    }

    private fun updateNodeCapture(deltaSeconds: Float) {
        val units = world.getAliveEntities()
            .filter { it.has(Unit::class.java) && it.has(CaptureBehavior::class.java) }

        world.entitiesWithComponent(ResourceNode::class.java).forEach { nodeEntity ->
            val nodeTransform = nodeEntity.get(Transform::class.java) ?: return@forEach
            val node = nodeEntity.get(ResourceNode::class.java) ?: return@forEach

            val player1Capturers = units.count { unit ->
                unit.owner() == OwnerType.PLAYER_1 &&
                        isInside(unit, nodeTransform, node.captureRadius)
            }

            val player2Capturers = units.count { unit ->
                unit.owner() == OwnerType.PLAYER_2 &&
                        isInside(unit, nodeTransform, node.captureRadius)
            }

            val captureSpeed = 0.35f * deltaSeconds

            when {
                player1Capturers > 0 && player2Capturers == 0 -> {
                    node.captureProgressPlayer1 = (node.captureProgressPlayer1 + captureSpeed * player1Capturers).coerceAtMost(1f)
                    node.captureProgressPlayer2 = (node.captureProgressPlayer2 - captureSpeed).coerceAtLeast(0f)

                    if (node.captureProgressPlayer1 >= 1f) {
                        node.capturedBy = 1
                    }
                }

                player2Capturers > 0 && player1Capturers == 0 -> {
                    node.captureProgressPlayer2 = (node.captureProgressPlayer2 + captureSpeed * player2Capturers).coerceAtMost(1f)
                    node.captureProgressPlayer1 = (node.captureProgressPlayer1 - captureSpeed).coerceAtLeast(0f)

                    if (node.captureProgressPlayer2 >= 1f) {
                        node.capturedBy = 2
                    }
                }
            }
        }
    }

    private fun updateUnitTargets() {
        val alive = world.getAliveEntities()
        val units = alive.filter { it.has(Unit::class.java) }

        units.forEach { unit ->
            val target = unit.get(Target::class.java) ?: return@forEach
            val unitTransform = unit.get(Transform::class.java) ?: return@forEach
            val combat = unit.get(CombatStats::class.java) ?: return@forEach

            val currentTarget = target.targetEntityId?.let { world.getEntity(it) }
            val currentTargetAlive = currentTarget?.get(Health::class.java)?.isDead == false

            if (currentTargetAlive) return@forEach

            val enemy = alive
                .filter { candidate ->
                    candidate.owner().isPlayer() &&
                            candidate.owner() != unit.owner() &&
                            candidate.has(Health::class.java)
                }
                .minByOrNull { candidate ->
                    val candidateTransform = candidate.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                    GameMath.distance(unitTransform, candidateTransform)
                }

            if (enemy != null) {
                val enemyTransform = enemy.get(Transform::class.java) ?: return@forEach
                val distance = GameMath.distance(unitTransform, enemyTransform)

                if (distance <= max(380f, combat.attackRange * 4f)) {
                    target.targetEntityId = enemy.id
                    target.targetX = enemyTransform.x
                    target.targetY = enemyTransform.y
                }
            }
        }
    }

    private fun updateMovement(deltaSeconds: Float, now: Long) {
        world.getAliveEntities()
            .filter { it.has(Unit::class.java) }
            .forEach { entity ->
                val transform = entity.get(Transform::class.java) ?: return@forEach
                val target = entity.get(Target::class.java) ?: return@forEach
                val combat = entity.get(CombatStats::class.java) ?: return@forEach
                val effects = entity.get(StatusEffects::class.java)

                if (effects?.isStunned(now) == true) return@forEach

                val targetEntity = target.targetEntityId?.let { world.getEntity(it) }

                if (targetEntity != null) {
                    val targetTransform = targetEntity.get(Transform::class.java) ?: return@forEach
                    val distance = GameMath.distance(transform, targetTransform)

                    if (distance > combat.attackRange * 0.85f) {
                        val speedMultiplier = if (effects?.isOverclocked(now) == true) 1.45f else 1f
                        GameMath.moveTowards(
                            transform = transform,
                            targetX = targetTransform.x,
                            targetY = targetTransform.y,
                            speed = combat.moveSpeed * speedMultiplier,
                            deltaSeconds = deltaSeconds
                        )
                    }
                } else {
                    val tx = target.targetX ?: return@forEach
                    val ty = target.targetY ?: return@forEach

                    val speedMultiplier = if (effects?.isOverclocked(now) == true) 1.45f else 1f
                    GameMath.moveTowards(
                        transform = transform,
                        targetX = tx,
                        targetY = ty,
                        speed = combat.moveSpeed * speedMultiplier,
                        deltaSeconds = deltaSeconds
                    )
                }
            }
    }

    private fun updateCombat(now: Long) {
        world.getAliveEntities()
            .filter { it.has(Unit::class.java) }
            .forEach { entity ->
                val transform = entity.get(Transform::class.java) ?: return@forEach
                val target = entity.get(Target::class.java) ?: return@forEach
                val combat = entity.get(CombatStats::class.java) ?: return@forEach
                val effects = entity.get(StatusEffects::class.java)

                if (effects?.isStunned(now) == true) return@forEach

                val targetEntity = target.targetEntityId?.let { world.getEntity(it) } ?: return@forEach

                if (targetEntity.owner() == entity.owner()) return@forEach

                val targetTransform = targetEntity.get(Transform::class.java) ?: return@forEach
                val targetHealth = targetEntity.get(Health::class.java) ?: return@forEach

                if (targetHealth.isDead) return@forEach

                val distance = GameMath.distance(transform, targetTransform)
                if (distance > combat.attackRange) return@forEach

                val cooldown = if (effects?.isOverclocked(now) == true) {
                    (combat.attackCooldownMillis * 0.7f).toLong()
                } else {
                    combat.attackCooldownMillis
                }

                if (now - combat.lastAttackAt < cooldown) return@forEach

                combat.lastAttackAt = now
                targetHealth.damage(combat.damage)
            }
    }

    private fun updateSupport(now: Long) {
        world.getAliveEntities()
            .filter { it.has(Unit::class.java) && it.has(SupportBehavior::class.java) }
            .forEach { support ->
                val transform = support.get(Transform::class.java) ?: return@forEach
                val combat = support.get(CombatStats::class.java) ?: return@forEach

                if (now - combat.lastAttackAt < 1200L) return@forEach

                val allyToHeal = world.getAliveEntities()
                    .filter {
                        it.owner() == support.owner() &&
                                it.id != support.id &&
                                it.has(Health::class.java)
                    }
                    .mapNotNull { ally ->
                        val allyTransform = ally.get(Transform::class.java) ?: return@mapNotNull null
                        val health = ally.get(Health::class.java) ?: return@mapNotNull null

                        if (health.current >= health.max) return@mapNotNull null

                        val distance = GameMath.distance(transform, allyTransform)
                        if (distance <= 145f) ally to health else null
                    }
                    .minByOrNull { it.second.current }

                if (allyToHeal != null) {
                    combat.lastAttackAt = now
                    allyToHeal.second.heal(10)
                }
            }
    }

    private fun updateCoreDeath() {
        val deadCore = world.entitiesWithComponent(Core::class.java)
            .firstOrNull { entity ->
                val health = entity.get(Health::class.java)
                health != null && health.isDead
            } ?: return

        val core = deadCore.get(Core::class.java) ?: return
        finishGame(loserPlayerIndex = core.playerIndex)
    }

    private fun castDeadlock(owner: OwnerType, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val radius = UnitRegistry.getConfig(UnitType.DEADLOCK).attackRange

        world.getAliveEntities()
            .filter {
                it.owner().isPlayer() &&
                        it.owner() != owner &&
                        it.has(StatusEffects::class.java)
            }
            .forEach { enemy ->
                val transform = enemy.get(Transform::class.java) ?: return@forEach
                val effects = enemy.get(StatusEffects::class.java) ?: return@forEach

                if (GameMath.distance(transform.x, transform.y, x, y) <= radius) {
                    effects.stunnedUntil = now + 2500L
                }
            }

        scope.launch {
            GameDispatcher.sendToAllPlayers(
                players,
                SystemMessageEvent("Deadlock blocked enemy execution flow")
            )
        }
    }

    private fun castOverclock(owner: OwnerType, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val radius = UnitRegistry.getConfig(UnitType.OVERCLOCK).attackRange

        world.getAliveEntities()
            .filter {
                it.owner() == owner &&
                        it.has(StatusEffects::class.java)
            }
            .forEach { ally ->
                val transform = ally.get(Transform::class.java) ?: return@forEach
                val effects = ally.get(StatusEffects::class.java) ?: return@forEach

                if (GameMath.distance(transform.x, transform.y, x, y) <= radius) {
                    effects.overclockUntil = now + 4500L
                }
            }

        scope.launch {
            GameDispatcher.sendToAllPlayers(
                players,
                SystemMessageEvent("Overclock boosted allied process throughput")
            )
        }
    }

    private fun finishGame(loserPlayerIndex: Int) {
        if (!finished.compareAndSet(false, true)) return

        val winnerPlayerIndex = if (loserPlayerIndex == 1) 2 else 1

        val loser = playerRuntimes.values.first { it.playerIndex == loserPlayerIndex }
        val winner = playerRuntimes.values.first { it.playerIndex == winnerPlayerIndex }

        scope.launch {
            sendSnapshot()

            GameDispatcher.sendToAllPlayers(
                players,
                GameOverEvent(
                    winnerPlayerId = winner.playerId,
                    loserPlayerId = loser.playerId,
                    reason = "Core destroyed"
                )
            )

            delay(1000L)
            stop()
            onFinished(roomId)
        }
    }

    private fun isInside(entity: Entity, target: Transform, radius: Float): Boolean {
        val transform = entity.get(Transform::class.java) ?: return false
        return GameMath.distance(transform, target) <= radius
    }

    fun stop() {
        gameLoopJob?.cancel()
        scope.cancel()
    }
}
