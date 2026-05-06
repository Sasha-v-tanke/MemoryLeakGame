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
import com.project.shared.engine.entities.components.Behavior
import com.project.shared.engine.entities.components.CaptureBehavior
import com.project.shared.engine.entities.components.CombatStats
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.DefenseBehavior
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.FactoryType
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.ResourceNodeType
import com.project.shared.engine.entities.components.StatusEffects
import com.project.shared.engine.entities.components.SupportBehavior
import com.project.shared.engine.entities.components.Target
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit as UnitComponent
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
import kotlin.math.ceil

class GameRoom(
    private val roomId: String,
    private val players: List<PlayerSession>,
    private val onRoomFinished: (String) -> kotlin.Unit
) {
    private data class ProductionOrder(
        val unitType: UnitType,
        val rallyX: Float,
        val rallyY: Float,
        val buildMillis: Long
    )

    private data class FactoryQueueState(
        val factoryId: Long,
        val owner: OwnerType,
        val type: FactoryType,
        val queue: ArrayDeque<ProductionOrder> = ArrayDeque(),
        var readyAt: Long = 0L
    )

    private val world = GameWorld()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val playerStatus = PlayerStatusHandler(players.map { it.playerId })
    private val started = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)

    private var gameLoopJob: Job? = null
    private var tick: Long = 0L
    private var lastIncomeAt: Long = System.currentTimeMillis()
    private val factoryQueues = mutableMapOf<Long, FactoryQueueState>()
    private val playerCardCooldowns = mutableMapOf<Int, MutableMap<UnitType, Long>>()

    private val playerRuntimes: Map<Int, PlayerRuntime> = players.mapIndexed { index, session ->
        session.playerId to PlayerRuntime(
            playerId = session.playerId,
            playerIndex = index + 1
        )
    }.toMap()

    init {
        DebugLog.info(
            "GameRoom created room=$roomId players=${players.map { it.playerId }}"
        )
    }

    fun setPlayerReady(playerReady: PlayerReadyRequest) {
        if (finished.get()) return

        DebugLog.info("Ready received room=$roomId player=${playerReady.playerId}")

        playerStatus.setPlayerReady(playerReady)

        if (playerStatus.isAllReady() && started.compareAndSet(false, true)) {
            DebugLog.info("All players ready room=$roomId. Starting game...")
            scope.launch {
                startGame()
            }
        }
    }

    fun playCard(request: PlayCardRequest): PlayCardResponse {
        DebugLog.card(
            "request room=$roomId started=${started.get()} finished=${finished.get()} " +
                    "player=${request.playerId} card=${request.unitType} x=${request.targetX} y=${request.targetY}"
        )

        if (!started.get()) {
            DebugLog.card("rejected: game is not started")
            return PlayCardResponse(false, "Game is not started yet")
        }

        if (finished.get()) {
            DebugLog.card("rejected: game is finished")
            return PlayCardResponse(false, "Game is already finished")
        }

        val runtime = playerRuntimes[request.playerId]
        if (runtime == null) {
            DebugLog.card("rejected: player not in room")
            return PlayCardResponse(false, "Player does not belong to this room")
        }

        val config = UnitRegistry.getConfig(request.unitType)
        val owner = OwnerType.fromPlayerIndex(runtime.playerIndex)
        val targetX = request.targetX.coerceIn(0f, GameConfig.worldWidth)
        val targetY = request.targetY.coerceIn(0f, GameConfig.worldHeight)
        val now = System.currentTimeMillis()

        val nextAvailableAt = playerCardCooldowns
            .getOrPut(request.playerId) { mutableMapOf() }
            .getOrDefault(request.unitType, 0L)

        if (now < nextAvailableAt) {
            val seconds = ((nextAvailableAt - now) / 100L).coerceAtLeast(1) / 10f
            return PlayCardResponse(false, "${config.displayName} cooldown: ${"%.1f".format(seconds)}s")
        }

        if (request.unitType == UnitType.DEADLOCK || request.unitType == UnitType.OVERCLOCK) {
            val supportFactory = findActiveFactory(owner, FactoryType.SUPPORT)
            if (supportFactory == null) {
                return PlayCardResponse(false, "Support Factory is required to cast this card")
            }
        }

        val spawnFactory = when (request.unitType) {
            UnitType.DEADLOCK,
            UnitType.OVERCLOCK -> null

            else -> {
                val requiredFactoryType = requiredFactoryFor(request.unitType)
                findFactoryQueue(owner, requiredFactoryType)
                    ?: return PlayCardResponse(false, "${requiredFactoryType.name.lowercase().replaceFirstChar { it.uppercase() }} Factory is destroyed")
            }
        }

        DebugLog.card(
            "resources before player=${request.playerId} memory=${runtime.memory} cpu=${runtime.cpu} " +
                    "costMemory=${config.costMemory} costCpu=${config.costCpu}"
        )

        if (runtime.memory < config.costMemory) {
            DebugLog.card("rejected: not enough Memory")
            return PlayCardResponse(false, "Not enough Memory")
        }

        if (runtime.cpu < config.costCpu) {
            DebugLog.card("rejected: not enough CPU")
            return PlayCardResponse(false, "Not enough CPU")
        }

        runtime.memory -= config.costMemory
        runtime.cpu -= config.costCpu
        playerCardCooldowns
            .getOrPut(request.playerId) { mutableMapOf() }[request.unitType] =
            now + cooldownMillisFor(request.unitType)

        when (request.unitType) {
            UnitType.DEADLOCK -> castDeadlock(owner, targetX, targetY)
            UnitType.OVERCLOCK -> castOverclock(owner, targetX, targetY)
            else -> {
                val queueState = spawnFactory!!
                if (queueState.queue.size >= 5) {
                    runtime.memory += config.costMemory
                    runtime.cpu += config.costCpu
                    playerCardCooldowns[request.playerId]?.remove(request.unitType)
                    return PlayCardResponse(false, "Factory queue is full")
                }

                queueState.queue.addLast(
                    ProductionOrder(
                        unitType = request.unitType,
                        rallyX = targetX,
                        rallyY = targetY,
                        buildMillis = buildMillisFor(request.unitType, queueState)
                    )
                )

                if (queueState.readyAt <= now) {
                    queueState.readyAt = now + queueState.queue.first().buildMillis
                }

                DebugLog.spawn(
                    "queued unit type=${request.unitType} factory=${queueState.factoryId} queue=${queueState.queue.size} readyAt=${queueState.readyAt}"
                )
            }
        }

        DebugLog.card(
            "accepted player=${request.playerId} memory=${runtime.memory} cpu=${runtime.cpu} entities=${world.getEntities().size}"
        )

        return when (request.unitType) {
            UnitType.DEADLOCK,
            UnitType.OVERCLOCK -> PlayCardResponse(true, "${config.displayName} cast")

            else -> {
                val queueSize = spawnFactory?.queue?.size ?: 0
                PlayCardResponse(true, "${config.displayName} queued (queue: $queueSize)")
            }
        }
    }

    private suspend fun startGame() {
        buildWorld()

        DebugLog.info(
            "World built room=$roomId entities=${world.getEntities().size} " +
                    "resources=${playerRuntimes.values.map { "p${it.playerIndex}:${it.memory}/${it.cpu}" }}"
        )

        sendSnapshot()
        GameDispatcher.notifyPlayersGameStarted(players)
        startGameLoop()
    }

    private fun buildWorld() {
        WorldConfig.objects.forEach { config ->
            val entity = EntityFactory.createWorldObject(world, config)
            val transform = entity.get(Transform::class.java)
            val factory = entity.get(Factory::class.java)

            if (factory != null) {
                factoryQueues[entity.id] = FactoryQueueState(
                    factoryId = entity.id,
                    owner = entity.owner(),
                    type = factory.factoryType
                )
            }

            DebugLog.spawn(
                "world object id=${entity.id} kind=${config.kind} owner=${config.owner} " +
                        "x=${transform?.x} y=${transform?.y} scale=${config.scale}"
            )
        }
    }

    private fun startGameLoop() {
        gameLoopJob = scope.launch {
            DebugLog.info("Game loop started room=$roomId")

            while (isActive && !finished.get()) {
                val start = System.currentTimeMillis()

                update(GameConfig.tickMillis / 1000f)

                if (tick % GameConfig.snapshotEveryTicks == 0L) {
                    sendSnapshot()
                }

                tick++

                val elapsed = System.currentTimeMillis() - start
                val delayTime = GameConfig.tickMillis - elapsed

                if (delayTime > 0) {
                    delay(delayTime)
                }
            }

            DebugLog.info("Game loop stopped room=$roomId")
        }
    }

    private suspend fun sendSnapshot() {
        val now = System.currentTimeMillis()

        val snapshot = GameStateSnapshotEvent(
            entities = world.getEntities().map { it.toState() },
            resources = playerRuntimes.values.associate { it.playerId to it.toResources() },
            cardCooldownsMs = playerCardCooldowns.mapValues { (playerId, cooldowns) ->
                cooldowns.mapValues { (_, nextAt) ->
                    (nextAt - now).coerceAtLeast(0L)
                }
            },
            factoryQueueSizes = factoryQueueSnapshot(),
            timestamp = now,
            tick = tick
        )

        GameDispatcher.sendToAllPlayers(players, snapshot)
    }

    private fun update(deltaSeconds: Float) {
        val now = System.currentTimeMillis()

        updateResourceIncome(now)
        processFactoryQueues(now)
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
            val beforeMemory = runtime.memory
            val beforeCpu = runtime.cpu

            runtime.memory += runtime.memoryIncome
            runtime.cpu += runtime.cpuIncome

            DebugLog.income(
                "player=${runtime.playerId} index=${runtime.playerIndex} " +
                        "memory $beforeMemory -> ${runtime.memory} (+${runtime.memoryIncome}) " +
                        "cpu $beforeCpu -> ${runtime.cpu} (+${runtime.cpuIncome})"
            )
        }
    }

    private fun recalculateIncome() {
        playerRuntimes.values.forEach { runtime ->
            runtime.memoryIncome = GameConfig.baseMemoryIncome
            runtime.cpuIncome = GameConfig.baseCpuIncome
        }

        world.entitiesWithComponent(Factory::class.java).forEach { factoryEntity ->
            val factory = factoryEntity.get(Factory::class.java) ?: return@forEach
            val health = factoryEntity.get(Health::class.java) ?: return@forEach
            if (health.isDead) return@forEach

            val playerIndex = factoryEntity.owner().playerIndexOrNull() ?: return@forEach
            val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == playerIndex } ?: return@forEach

            when (factory.factoryType) {
                FactoryType.BASIC -> runtime.memoryIncome += GameConfig.basicFactoryMemoryIncomeBonus
                FactoryType.SUPPORT -> runtime.cpuIncome += GameConfig.supportFactoryCpuIncomeBonus
            }
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

    private fun processFactoryQueues(now: Long) {
        factoryQueues.values.forEach { queueState ->
            val factoryEntity = world.getEntity(queueState.factoryId)
            val factoryAlive = factoryEntity
                ?.get(Health::class.java)
                ?.isDead == false

            if (!factoryAlive) {
                if (queueState.queue.isNotEmpty()) {
                    queueState.queue.clear()
                    queueState.readyAt = 0L
                }
                return@forEach
            }

            if (queueState.queue.isEmpty() || now < queueState.readyAt) return@forEach

            val order = queueState.queue.removeFirst()
            val spawnPoint = spawnPointNearFactory(factoryEntity!!, queueState.owner)

            val entity = UnitFactory.createUnit(
                world = world,
                unitType = order.unitType,
                owner = queueState.owner,
                spawnX = spawnPoint.first,
                spawnY = spawnPoint.second,
                rallyX = order.rallyX,
                rallyY = order.rallyY
            )

            DebugLog.spawn(
                "produced id=${entity.id} type=${order.unitType} factory=${queueState.factoryId} remaining=${queueState.queue.size}"
            )

            queueState.readyAt = if (queueState.queue.isEmpty()) {
                0L
            } else {
                now + queueState.queue.first().buildMillis
            }
        }
    }

    private fun findBestNodeToCapture(unit: Entity, unitTransform: Transform): Entity? {
        val playerIndex = unit.owner().playerIndexOrNull() ?: return null

        return world.entitiesWithComponent(ResourceNode::class.java)
            .filter { nodeEntity ->
                val node = nodeEntity.get(ResourceNode::class.java) ?: return@filter false
                // берём ноды, которые либо нейтральны, либо захвачены врагом
                node.capturedBy != playerIndex
            }
            .minByOrNull { nodeEntity ->
                val nodeTransform = nodeEntity.get(Transform::class.java)
                    ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(unitTransform, nodeTransform)
            }
    }

    private fun updateNodeCapture(deltaSeconds: Float) {
        val units = world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) && it.has(CaptureBehavior::class.java) }

        world.entitiesWithComponent(ResourceNode::class.java).forEach { nodeEntity ->
            val nodeTransform = nodeEntity.get(Transform::class.java) ?: return@forEach
            val node = nodeEntity.get(ResourceNode::class.java) ?: return@forEach

            val oldOwner = node.capturedBy
            val oldP1 = node.captureProgressPlayer1
            val oldP2 = node.captureProgressPlayer2

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
                    node.captureProgressPlayer1 =
                        (node.captureProgressPlayer1 + captureSpeed * player1Capturers).coerceAtMost(1f)
                    node.captureProgressPlayer2 =
                        (node.captureProgressPlayer2 - captureSpeed).coerceAtLeast(0f)

                    if (node.captureProgressPlayer1 >= 1f) {
                        node.capturedBy = 1
                    }
                }

                player2Capturers > 0 && player1Capturers == 0 -> {
                    node.captureProgressPlayer2 =
                        (node.captureProgressPlayer2 + captureSpeed * player2Capturers).coerceAtMost(1f)
                    node.captureProgressPlayer1 =
                        (node.captureProgressPlayer1 - captureSpeed).coerceAtLeast(0f)

                    if (node.captureProgressPlayer2 >= 1f) {
                        node.capturedBy = 2
                    }
                }
            }

            if (
                oldOwner != node.capturedBy ||
                kotlin.math.abs(oldP1 - node.captureProgressPlayer1) > 0.05f ||
                kotlin.math.abs(oldP2 - node.captureProgressPlayer2) > 0.05f
            ) {
                DebugLog.capture(
                    "node=${nodeEntity.id} type=${node.nodeType} owner ${oldOwner} -> ${node.capturedBy} " +
                            "p1=${"%.2f".format(node.captureProgressPlayer1)} p2=${"%.2f".format(node.captureProgressPlayer2)} " +
                            "capturers=$player1Capturers/$player2Capturers"
                )
            }
        }
    }

    private fun updateUnitTargets() {
        val alive = world.getAliveEntities()
        val units = alive.filter { it.has(UnitComponent::class.java) }

        units.forEach { unit ->
            val target = unit.get(Target::class.java) ?: return@forEach
            val unitTransform = unit.get(Transform::class.java) ?: return@forEach
            val combat = unit.get(CombatStats::class.java) ?: return@forEach
            val unitComponent = unit.get(UnitComponent::class.java) ?: return@forEach

            val currentTarget = target.targetEntityId?.let { world.getEntity(it) }
            val currentTargetAlive = currentTarget?.get(Health::class.java)?.isDead == false
            val shouldDropCurrent = currentTargetAlive &&
                    shouldClearTarget(unit, unitComponent, currentTarget, unitTransform, combat)
            val enemy = findBestTarget(
                attacker = unitComponent,
                source = unit,
                sourceTransform = unitTransform,
                combat = combat,
                candidates = alive
            )

            if (enemy == null) {
                if (!currentTargetAlive || shouldDropCurrent) {
                    if (unit.has(CaptureBehavior::class.java)) {
                        val node = findBestNodeToCapture(unit, unitTransform)
                        if (node != null) {
                            setEntityTarget(target, node)
                            return@forEach
                        }
                    }
                    restoreRallyTarget(unit, target, unitTransform)
                }
                return@forEach
            }

            if (!currentTargetAlive || shouldDropCurrent || shouldReplaceTarget(unitComponent, unitTransform, combat, currentTarget, enemy)) {
                setEntityTarget(target, enemy)
            }
        }
    }

    private fun findBestTarget(
        attacker: UnitComponent,
        source: Entity,
        sourceTransform: Transform,
        combat: CombatStats,
        candidates: List<Entity>
    ): Entity? {
        return candidates
            .filter { candidate ->
                candidate.owner().isPlayer() &&
                        candidate.owner() != source.owner() &&
                        candidate.has(Health::class.java) &&
                        isValidTargetFor(attacker, source, sourceTransform, combat, candidate)
            }
            .minByOrNull { candidate ->
                val candidateTransform = candidate.get(Transform::class.java)
                    ?: return@minByOrNull Float.MAX_VALUE
                targetScore(attacker, candidate, GameMath.distance(sourceTransform, candidateTransform))
            }
    }

    private fun setEntityTarget(target: Target, enemy: Entity) {
        val enemyTransform = enemy.get(Transform::class.java) ?: return
        target.targetEntityId = enemy.id
        target.targetX = enemyTransform.x
        target.targetY = enemyTransform.y
    }

    private fun shouldReplaceTarget(
        attacker: UnitComponent,
        unitTransform: Transform,
        combat: CombatStats,
        currentTarget: Entity?,
        newTarget: Entity
    ): Boolean {
        if (currentTarget == null) return true
        if (!isValidTargetFor(attacker, null, unitTransform, combat, currentTarget)) return true

        val currentTransform = currentTarget.get(Transform::class.java) ?: return true
        val newTransform = newTarget.get(Transform::class.java) ?: return false
        val currentScore = targetScore(attacker, currentTarget, GameMath.distance(unitTransform, currentTransform))
        val newScore = targetScore(attacker, newTarget, GameMath.distance(unitTransform, newTransform))

        return newScore + 12f < currentScore
    }

    private fun shouldClearTarget(
        unit: Entity,
        attacker: UnitComponent,
        currentTarget: Entity?,
        unitTransform: Transform,
        combat: CombatStats
    ): Boolean {
        if (currentTarget == null) return false
        if (!isValidTargetFor(attacker, unit, unitTransform, combat, currentTarget)) return true

        if (attacker.role == UnitRole.DEFENSE || attacker.role == UnitRole.SUPPORT) {
            val rally = rallyPointFor(unit, unitTransform)
            val targetTransform = currentTarget.get(Transform::class.java) ?: return true
            return GameMath.distance(targetTransform.x, targetTransform.y, rally.first, rally.second) > guardRadius(attacker)
        }

        return false
    }

    private fun restoreRallyTarget(unit: Entity, target: Target, unitTransform: Transform) {
        val rally = rallyPointFor(unit, unitTransform)
        target.targetEntityId = null
        target.targetX = rally.first
        target.targetY = rally.second
    }

    private fun rallyPointFor(unit: Entity, fallbackTransform: Transform): Pair<Float, Float> {
        val behavior: Behavior? = unit.get(CaptureBehavior::class.java)
            ?: unit.get(SupportBehavior::class.java)
            ?: unit.get(DefenseBehavior::class.java)
            ?: unit.get(AttackBehavior::class.java)

        return (behavior?.targetX ?: fallbackTransform.x) to (behavior?.targetY ?: fallbackTransform.y)
    }

    private fun isValidTargetFor(
        attacker: UnitComponent,
        source: Entity?,
        sourceTransform: Transform,
        combat: CombatStats,
        candidate: Entity
    ): Boolean {
        val candidateTransform = candidate.get(Transform::class.java) ?: return false
        val distance = GameMath.distance(sourceTransform, candidateTransform)
        val assignedAreaOk = if (source != null && (attacker.role == UnitRole.DEFENSE || attacker.role == UnitRole.SUPPORT)) {
            val rally = rallyPointFor(source, sourceTransform)
            GameMath.distance(candidateTransform.x, candidateTransform.y, rally.first, rally.second) <= guardRadius(attacker)
        } else {
            true
        }

        return when (attacker.typeName) {
            UnitType.ALLOCATOR,
            UnitType.CACHE_RUNNER -> candidate.has(UnitComponent::class.java) && distance <= combat.attackRange * 1.65f

            UnitType.GARBAGE_COLLECTOR,
            UnitType.PATCH_HEALER -> candidate.has(UnitComponent::class.java) &&
                    distance <= combat.attackRange * 1.35f &&
                    assignedAreaOk

            UnitType.THREAD_GUARD,
            UnitType.FIREWALL -> candidate.has(UnitComponent::class.java) &&
                    distance <= guardRadius(attacker) &&
                    assignedAreaOk

            UnitType.INJECTOR -> candidate.has(Factory::class.java) ||
                    candidate.has(Core::class.java) ||
                    candidate.has(UnitComponent::class.java)

            UnitType.COROUTINE_ARCHER -> candidate.has(UnitComponent::class.java) ||
                    candidate.has(Factory::class.java) ||
                    candidate.has(Core::class.java)

            UnitType.DEADLOCK,
            UnitType.OVERCLOCK -> false
        }
    }

    private fun targetScore(attacker: UnitComponent, candidate: Entity, distance: Float): Float {
        val base = when (attacker.typeName) {
            UnitType.ALLOCATOR,
            UnitType.CACHE_RUNNER -> when {
                candidate.has(UnitComponent::class.java) -> 20f
                else -> Float.MAX_VALUE
            }

            UnitType.GARBAGE_COLLECTOR,
            UnitType.PATCH_HEALER -> when {
                candidate.has(UnitComponent::class.java) -> 25f
                else -> Float.MAX_VALUE
            }

            UnitType.THREAD_GUARD,
            UnitType.FIREWALL -> when {
                candidate.has(UnitComponent::class.java) -> 10f
                else -> Float.MAX_VALUE
            }

            UnitType.INJECTOR -> when {
                candidate.has(Factory::class.java) -> 5f
                candidate.has(Core::class.java) -> 8f
                candidate.has(UnitComponent::class.java) -> 35f
                else -> Float.MAX_VALUE
            }

            UnitType.COROUTINE_ARCHER -> when {
                candidate.has(UnitComponent::class.java) -> 0f
                candidate.has(Factory::class.java) -> 35f
                candidate.has(Core::class.java) -> 45f
                else -> Float.MAX_VALUE
            }

            UnitType.DEADLOCK,
            UnitType.OVERCLOCK -> Float.MAX_VALUE
        }

        return base + distance / 10f
    }

    private fun guardRadius(attacker: UnitComponent): Float {
        return when (attacker.typeName) {
            UnitType.FIREWALL -> 280f
            UnitType.THREAD_GUARD -> 230f
            else -> 170f
        }
    }

    private fun updateMovement(deltaSeconds: Float, now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
            .forEach { entity ->
                val transform = entity.get(Transform::class.java) ?: return@forEach
                val target = entity.get(Target::class.java) ?: return@forEach
                val combat = entity.get(CombatStats::class.java) ?: return@forEach
                val effects = entity.get(StatusEffects::class.java)

                if (effects?.isStunned(now) == true) return@forEach

                val beforeX = transform.x
                val beforeY = transform.y

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

                if (kotlin.math.abs(beforeX - transform.x) > 0.1f || kotlin.math.abs(beforeY - transform.y) > 0.1f) {
                    DebugLog.movement(
                        "entity=${entity.id} owner=${entity.owner()} " +
                                "from=${"%.1f".format(beforeX)},${"%.1f".format(beforeY)} " +
                                "to=${"%.1f".format(transform.x)},${"%.1f".format(transform.y)} " +
                                "target=${target.targetEntityId ?: "${target.targetX},${target.targetY}"}"
                    )
                }
            }
    }

    private fun updateCombat(now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
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
                val before = targetHealth.current
                targetHealth.damage(combat.damage)

                DebugLog.combat(
                    "attacker=${entity.id} target=${targetEntity.id} damage=${combat.damage} hp $before -> ${targetHealth.current}"
                )
            }
    }

    private fun updateSupport(now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) && it.has(SupportBehavior::class.java) }
            .forEach { support ->
                val transform = support.get(Transform::class.java) ?: return@forEach
                val combat = support.get(CombatStats::class.java) ?: return@forEach
                val unit = support.get(UnitComponent::class.java) ?: return@forEach

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
                    val before = allyToHeal.second.current
                    combat.lastAttackAt = now
                    val healAmount = when (unit.typeName) {
                        UnitType.PATCH_HEALER -> 14
                        UnitType.GARBAGE_COLLECTOR -> 11
                        else -> 10
                    }

                    allyToHeal.second.heal(healAmount)

                    DebugLog.combat(
                        "support=${support.id} healed=${allyToHeal.first.id} hp $before -> ${allyToHeal.second.current}"
                    )
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

        var affected = 0

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
                    affected++
                }
            }

        DebugLog.card("Deadlock cast owner=$owner x=$x y=$y affected=$affected")

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

        var affected = 0

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
                    affected++
                }
            }

        DebugLog.card("Overclock cast owner=$owner x=$x y=$y affected=$affected")

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

        DebugLog.info("Game finished room=$roomId winner=${winner.playerId} loser=${loser.playerId}")

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
            onRoomFinished(roomId)
        }
    }

    private fun isInside(entity: Entity, target: Transform, radius: Float): Boolean {
        val transform = entity.get(Transform::class.java) ?: return false
        return GameMath.distance(transform, target) <= radius
    }

    private fun cooldownMillisFor(unitType: UnitType): Long {
        return when (unitType) {
            UnitType.ALLOCATOR -> 3500L
            UnitType.CACHE_RUNNER -> 3000L
            UnitType.GARBAGE_COLLECTOR -> 5200L
            UnitType.PATCH_HEALER -> 4500L
            UnitType.THREAD_GUARD -> 6300L
            UnitType.FIREWALL -> 7600L
            UnitType.INJECTOR -> 6800L
            UnitType.COROUTINE_ARCHER -> 6400L
            UnitType.DEADLOCK -> 10000L
            UnitType.OVERCLOCK -> 8500L
        }
    }

    private fun buildMillisFor(unitType: UnitType, queueState: FactoryQueueState): Long {
        val config = UnitRegistry.getConfig(unitType)
        val factoryEntity = world.getEntity(queueState.factoryId)
        val multiplier = factoryEntity
            ?.get(Factory::class.java)
            ?.productionMultiplier
            ?: 1f

        val raw = (config.buildTime * 1000f) / multiplier.coerceAtLeast(0.1f)
        return ceil(raw.toDouble()).toLong().coerceAtLeast(900L)
    }

    private fun findFactoryQueue(owner: OwnerType, type: FactoryType): FactoryQueueState? {
        return factoryQueues.values.firstOrNull { queueState ->
            if (queueState.owner != owner || queueState.type != type) return@firstOrNull false
            val entity = world.getEntity(queueState.factoryId) ?: return@firstOrNull false
            val health = entity.get(Health::class.java) ?: return@firstOrNull false
            !health.isDead
        }
    }

    private fun factoryQueueSnapshot(): Map<Int, Map<FactoryType, Int>> {
        return playerRuntimes.values.associate { runtime ->
            val owner = OwnerType.fromPlayerIndex(runtime.playerIndex)

            val byFactory = FactoryType.entries.associateWith { factoryType ->
                factoryQueues.values
                    .firstOrNull { it.owner == owner && it.type == factoryType }
                    ?.queue
                    ?.size
                    ?: 0
            }

            runtime.playerId to byFactory
        }
    }

    private fun requiredFactoryFor(unitType: UnitType): FactoryType {
        return when (unitType) {
            UnitType.ALLOCATOR,
            UnitType.INJECTOR,
            UnitType.CACHE_RUNNER,
            UnitType.COROUTINE_ARCHER -> FactoryType.BASIC

            UnitType.GARBAGE_COLLECTOR,
            UnitType.THREAD_GUARD,
            UnitType.FIREWALL,
            UnitType.PATCH_HEALER -> FactoryType.SUPPORT

            UnitType.DEADLOCK,
            UnitType.OVERCLOCK -> FactoryType.SUPPORT
        }
    }

    private fun findActiveFactory(owner: OwnerType, type: FactoryType): Entity? {
        return world.getAliveEntities()
            .filter { entity ->
                entity.owner() == owner &&
                        entity.has(Factory::class.java)
            }
            .firstOrNull { entity ->
                entity.get(Factory::class.java)?.factoryType == type
            }
    }

    private fun spawnPointNearFactory(factory: Entity, owner: OwnerType): Pair<Float, Float> {
        val transform = factory.get(Transform::class.java)
            ?: return GameConfig.worldWidth / 2f to GameConfig.worldHeight / 2f

        val lateral = ((tick % 5L) - 2L).toFloat() * 12f
        val forward = if (owner == OwnerType.PLAYER_1) 58f else -58f

        val spawnX = (transform.x + lateral).coerceIn(16f, GameConfig.worldWidth - 16f)
        val spawnY = (transform.y + forward).coerceIn(16f, GameConfig.worldHeight - 16f)
        return spawnX to spawnY
    }

    fun stop() {
        DebugLog.info("Stopping room=$roomId")
        gameLoopJob?.cancel()
        scope.cancel()
    }
}
