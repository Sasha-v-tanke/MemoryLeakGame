package com.project.server.engine

import com.project.server.models.PlayerRuntime
import com.project.server.models.PlayerSession
import com.project.server.service.GameDispatcher
import com.project.shared.api.events.GameOverEvent
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.events.SystemMessageEvent
import com.project.shared.api.game.BuildFactoryRequest
import com.project.shared.api.game.BuildFactoryResponse
import com.project.shared.api.game.ForfeitMatchRequest
import com.project.shared.api.game.ForfeitMatchResponse
import com.project.shared.api.game.PlayCardRequest
import com.project.shared.api.game.PlayCardResponse
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.engine.MatchStats
import com.project.shared.engine.PlayerMatchStats
import com.project.shared.engine.WorldTextEvent
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
import com.project.shared.engine.entities.components.ProcessPhase
import com.project.shared.engine.entities.components.ProcessState
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
import kotlin.math.cos
import kotlin.math.sin

class GameRoom(
    private val roomId: String,
    private val players: List<PlayerSession>,
    private val onRoomFinished: (String) -> kotlin.Unit
) {
    private data class ProductionOrder(
        val unitType: UnitType,
        val manualX: Float?,
        val manualY: Float?,
        val buildMillis: Long
    )

    private data class FactoryQueueState(
        val factoryId: Long,
        val owner: OwnerType,
        val type: FactoryType,
        val queue: ArrayDeque<ProductionOrder> = ArrayDeque(),
        var readyAt: Long = 0L
    )

    private data class MutableStats(
        val unitsQueued: MutableMap<UnitType, Int> = mutableMapOf(),
        val unitsProduced: MutableMap<UnitType, Int> = mutableMapOf(),
        val unitsLost: MutableMap<UnitType, Int> = mutableMapOf(),
        val enemyUnitsKilled: MutableMap<UnitType, Int> = mutableMapOf(),
        var memoryAllocated: Int = 0,
        var memoryFreed: Int = 0,
        var factoriesBuilt: Int = 0,
        val spellsCast: MutableMap<UnitType, Int> = mutableMapOf()
    ) {
        fun snapshot(): PlayerMatchStats {
            return PlayerMatchStats(
                unitsQueued = unitsQueued.toMap(),
                unitsProduced = unitsProduced.toMap(),
                unitsLost = unitsLost.toMap(),
                enemyUnitsKilled = enemyUnitsKilled.toMap(),
                memoryAllocated = memoryAllocated,
                memoryFreed = memoryFreed,
                factoriesBuilt = factoriesBuilt,
                spellsCast = spellsCast.toMap()
            )
        }
    }

    private val world = GameWorld()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val playerStatus = PlayerStatusHandler(players.map { it.playerId })
    private val started = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)

    private var gameLoopJob: Job? = null
    private var tick: Long = 0L
    private var lastIncomeAt: Long = System.currentTimeMillis()
    private var textEventId = 1L

    private val factoryQueues = mutableMapOf<Long, FactoryQueueState>()
    private val playerCardCooldowns = mutableMapOf<Int, MutableMap<UnitType, Long>>()
    private val textEvents = ArrayDeque<WorldTextEvent>()
    private val stats = mutableMapOf<Int, MutableStats>()

    private val playerRuntimes: Map<Int, PlayerRuntime> = players.mapIndexed { index, session ->
        session.playerId to PlayerRuntime(
            playerId = session.playerId,
            playerIndex = index + 1
        )
    }.toMap()

    init {
        players.forEach { stats[it.playerId] = MutableStats() }
    }

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
        if (!started.get()) return PlayCardResponse(false, "Game is not started yet")
        if (finished.get()) return PlayCardResponse(false, "Game is already finished")

        val runtime = playerRuntimes[request.playerId]
            ?: return PlayCardResponse(false, "Player does not belong to this room")

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

        val isManualCard = isManualTargetCard(request.unitType)

        val spawnFactory: FactoryQueueState? = when {
            isManualCard -> null
            else -> {
                val type = requiredFactoryFor(request.unitType)
                findFactoryQueue(owner, type)
                    ?: return PlayCardResponse(false, "${type.name.lowercase().replaceFirstChar { it.uppercase() }} Factory is required")
            }
        }

        if (runtime.memory < config.costMemory) return PlayCardResponse(false, "Not enough allocated Memory")
        if (runtime.cpu < config.costCpu) return PlayCardResponse(false, "Not enough CPU")

        runtime.memory -= config.costMemory
        runtime.cpu -= config.costCpu
        playerCardCooldowns.getOrPut(request.playerId) { mutableMapOf() }[request.unitType] = now + cooldownMillisFor(request.unitType)

        when (request.unitType) {
            UnitType.DEADLOCK -> {
                inc(stats[request.playerId]?.spellsCast, request.unitType)
                castDeadlock(owner, targetX, targetY)
                return PlayCardResponse(true, "Deadlock cast at selected area")
            }

            UnitType.OVERCLOCK -> {
                inc(stats[request.playerId]?.spellsCast, request.unitType)
                castOverclock(owner, targetX, targetY)
                return PlayCardResponse(true, "Overclock cast at selected area")
            }

            UnitType.NULL_POINTER -> {
                inc(stats[request.playerId]?.spellsCast, request.unitType)
                castNullPointer(owner, targetX, targetY)
                return PlayCardResponse(true, "Null Pointer dereferenced at selected point")
            }

            else -> {
                val queue = spawnFactory ?: return PlayCardResponse(false, "Factory is not available")
                if (queue.queue.size >= queueLimit(owner, queue.type)) {
                    runtime.memory += config.costMemory
                    runtime.cpu += config.costCpu
                    playerCardCooldowns[request.playerId]?.remove(request.unitType)
                    return PlayCardResponse(false, "Factory queue is full")
                }

                queue.queue.addLast(
                    ProductionOrder(
                        unitType = request.unitType,
                        manualX = null,
                        manualY = null,
                        buildMillis = buildMillisFor(request.unitType, queue)
                    )
                )

                if (queue.readyAt <= now) queue.readyAt = now + queue.queue.first().buildMillis

                inc(stats[request.playerId]?.unitsQueued, request.unitType)
                return PlayCardResponse(true, "${config.displayName} queued. It will choose target automatically.")
            }
        }
    }

    fun buildFactory(request: BuildFactoryRequest): BuildFactoryResponse {
        if (!started.get()) return BuildFactoryResponse(false, "Game is not started yet")
        if (finished.get()) return BuildFactoryResponse(false, "Game is already finished")

        val runtime = playerRuntimes[request.playerId]
            ?: return BuildFactoryResponse(false, "Player does not belong to this room")

        val owner = OwnerType.fromPlayerIndex(runtime.playerIndex)
        val memoryCost = when (request.factoryType) {
            FactoryType.BASIC -> GameConfig.basicFactoryBuildMemoryCost
            FactoryType.SUPPORT -> GameConfig.supportFactoryBuildMemoryCost
        }
        val cpuCost = when (request.factoryType) {
            FactoryType.BASIC -> GameConfig.basicFactoryBuildCpuCost
            FactoryType.SUPPORT -> GameConfig.supportFactoryBuildCpuCost
        }

        if (runtime.memory < memoryCost) return BuildFactoryResponse(false, "Not enough allocated Memory")
        if (runtime.cpu < cpuCost) return BuildFactoryResponse(false, "Not enough CPU")

        val core = findCore(owner) ?: return BuildFactoryResponse(false, "Core not found")
        val position = nextFactoryPosition(owner, request.factoryType)

        runtime.memory -= memoryCost
        runtime.cpu -= cpuCost
        runtime.factoriesBuilt += 1

        val factory = EntityFactory.createBuiltFactory(world, owner, request.factoryType, position.first, position.second)
        factoryQueues[factory.id] = FactoryQueueState(factory.id, owner, request.factoryType)
        recalculateFactoryMultipliers(owner, request.factoryType)
        stats[request.playerId]?.factoriesBuilt = stats[request.playerId]?.factoriesBuilt?.plus(1) ?: 1

        val t = core.get(Transform::class.java)
        if (t != null) pushText(owner, t.x, t.y + 90f, "Factory scaled: more parallel production")

        return BuildFactoryResponse(true, "${request.factoryType.name.lowercase().replaceFirstChar { it.uppercase() }} Factory built")
    }

    fun forfeit(request: ForfeitMatchRequest): ForfeitMatchResponse {
        if (finished.get()) return ForfeitMatchResponse(false, "Match already finished")
        val runtime = playerRuntimes[request.playerId]
            ?: return ForfeitMatchResponse(false, "Player does not belong to this room")

        finishGame(runtime.playerIndex, "Forfeit: instance owner terminated the match")
        return ForfeitMatchResponse(true, "Forfeit accepted")
    }

    private suspend fun startGame() {
        buildWorld()
        sendSnapshot()
        GameDispatcher.notifyPlayersGameStarted(players)
        startGameLoop()
    }

    private fun buildWorld() {
        WorldConfig.objects.forEach { config ->
            val entity = EntityFactory.createWorldObject(world, config)
            val factory = entity.get(Factory::class.java)
            if (factory != null) {
                factoryQueues[entity.id] = FactoryQueueState(entity.id, entity.owner(), factory.factoryType)
            }
        }
    }

    private fun startGameLoop() {
        gameLoopJob = scope.launch {
            while (isActive && !finished.get()) {
                val start = System.currentTimeMillis()
                update(GameConfig.tickMillis / 1000f)
                if (tick % GameConfig.snapshotEveryTicks == 0L) sendSnapshot()
                tick++
                val elapsed = System.currentTimeMillis() - start
                val delayTime = GameConfig.tickMillis - elapsed
                if (delayTime > 0) delay(delayTime)
            }
        }
    }

    private suspend fun sendSnapshot() {
        val now = System.currentTimeMillis()
        cleanupTextEvents(now)

        val snapshot = GameStateSnapshotEvent(
            entities = world.getEntities().map { it.toState() },
            resources = playerRuntimes.values.associate { it.playerId to it.toResources() },
            cardCooldownsMs = playerCardCooldowns.mapValues { (_, cooldowns) ->
                cooldowns.mapValues { (_, nextAt) -> (nextAt - now).coerceAtLeast(0L) }
            },
            factoryQueueSizes = factoryQueueSnapshot(),
            stats = statsSnapshot(),
            textEvents = textEvents.toList(),
            timestamp = now,
            tick = tick
        )

        GameDispatcher.sendToAllPlayers(players, snapshot)
    }
    private fun updateCpuShares() {
        world.entitiesWithComponent(ResourceNode::class.java)
            .filter { it.get(ResourceNode::class.java)?.nodeType == ResourceNodeType.CPU }
            .forEach { nodeEntity ->
                val node = nodeEntity.get(ResourceNode::class.java) ?: return@forEach
                val nodeTransform = nodeEntity.get(Transform::class.java) ?: return@forEach

                val cpuWorkers = world.getAliveEntities()
                    .filter { worker ->
                        val unit = worker.get(UnitComponent::class.java) ?: return@filter false
                        if (unit.cpuRedirectPower <= 0f) return@filter false
                        val transform = worker.get(Transform::class.java) ?: return@filter false
                        GameMath.distance(transform, nodeTransform) <= node.captureRadius
                    }

                val totalPower = cpuWorkers.sumOf { worker ->
                    (worker.get(UnitComponent::class.java)?.cpuRedirectPower ?: 0f).toDouble()
                }.toFloat()

                if (totalPower <= 0f) return@forEach

                val p1Power = cpuWorkers
                    .filter { it.owner() == OwnerType.PLAYER_1 }
                    .sumOf { (it.get(UnitComponent::class.java)?.cpuRedirectPower ?: 0f).toDouble() }
                    .toFloat()

                val p2Power = cpuWorkers
                    .filter { it.owner() == OwnerType.PLAYER_2 }
                    .sumOf { (it.get(UnitComponent::class.java)?.cpuRedirectPower ?: 0f).toDouble() }
                    .toFloat()

                node.captureProgressPlayer1 = (p1Power / totalPower).coerceIn(0f, 1f)
                node.captureProgressPlayer2 = (p2Power / totalPower).coerceIn(0f, 1f)

                val sum = node.captureProgressPlayer1 + node.captureProgressPlayer2
                if (sum != 1f && sum > 0f) {
                    node.captureProgressPlayer1 /= sum
                    node.captureProgressPlayer2 /= sum
                }

                node.capturedBy = when {
                    node.captureProgressPlayer1 > node.captureProgressPlayer2 -> 1
                    node.captureProgressPlayer2 > node.captureProgressPlayer1 -> 2
                    else -> null
                }
            }
    }

    private fun update(deltaSeconds: Float) {
        val now = System.currentTimeMillis()
        updateCpuShares()
        updateResourceIncome(now)
        processFactoryQueues(now)
        assignAutonomousTargets(now)
        updateMemoryWorkers(deltaSeconds, now)
        updateGarbageCollectorWork(now)
        updateSpecialProcesses(now)
        updateAuras(now)
        updateUnitTargets()
        updateMovement(deltaSeconds, now)
        updateCombat(now)
        updatePatchHealer(now)
        updateDeaths(now)
        updateCoreDeath()
    }

    private fun assignAutonomousTargets(now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
            .forEach { entity ->
                val unit = entity.get(UnitComponent::class.java) ?: return@forEach
                val target = entity.get(Target::class.java) ?: return@forEach
                val transform = entity.get(Transform::class.java) ?: return@forEach

                if (isManualTargetCard(unit.typeName)) return@forEach

                when (unit.typeName) {
                    UnitType.CACHE_RUNNER -> assignCacheRunnerTarget(entity, target, transform)
                    UnitType.GARBAGE_COLLECTOR -> assignGarbageCollectorTarget(entity, target, transform)
                    UnitType.PATCH_HEALER -> assignPatchHealerTarget(entity, target, transform)
                    UnitType.THREAD_GUARD,
                    UnitType.FIREWALL,
                    UnitType.MUTEX,
                    UnitType.SEMAPHORE,
                    UnitType.EXCEPTION_HANDLER -> assignDefensivePosition(entity, target, transform)
                    UnitType.INJECTOR -> assignStructureAttackTarget(entity, target, transform)
                    UnitType.COROUTINE_ARCHER,
                    UnitType.LOOP,
                    UnitType.RECURSIVE_CALL,
                    UnitType.STACK_FRAME -> assignCombatTarget(entity, target, transform)
                    UnitType.POINTER,
                    UnitType.OBSERVER -> assignObserverPosition(entity, target, transform, now)
                    UnitType.DEADLOCK,
                    UnitType.OVERCLOCK,
                    UnitType.NULL_POINTER -> {}
                    UnitType.HEAP_BLOCK -> assignDefensivePosition(entity, target, transform)
                    UnitType.ALLOCATOR,
                    UnitType.MEMORY_POOL,
                    UnitType.DMA_CONTROLLER,
                    UnitType.BUFFER -> assignAllocatorTarget(entity, target, transform)
                    UnitType.CPU_SCHEDULER,
                    UnitType.LOAD_BALANCER,
                    UnitType.INTERRUPT_HANDLER -> assignAllocatorTarget(entity, target, transform)
                }
            }
    }

    private fun assignAllocatorTarget(entity: Entity, target: Target, transform: Transform) {
        val unit = entity.get(UnitComponent::class.java) ?: return

        val node = when {
            unit.memoryWorkPower > 0f && unit.cpuRedirectPower <= 0f -> bestMemoryNode(transform)
            unit.cpuRedirectPower > 0f && unit.memoryWorkPower <= 0f -> bestCpuNodeForWorker(entity.owner(), transform)
            unit.cpuRedirectPower > 0f && unit.memoryWorkPower > 0f -> {
                val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == entity.owner().playerIndexOrNull() }
                if (runtime == null || runtime.memory < 18) bestMemoryNode(transform) else bestCpuNodeForWorker(entity.owner(), transform)
            }
            else -> null
        } ?: return

        setEntityTarget(target, node)
    }

    private fun assignCacheRunnerTarget(entity: Entity, target: Target, transform: Transform) {
        val node = bestCpuNodeForWorker(entity.owner(), transform)
        if (node != null) setEntityTarget(target, node)
        else assignCombatTarget(entity, target, transform)
    }

    private fun assignGarbageCollectorTarget(entity: Entity, target: Target, transform: Transform) {
        val dead = nearestDeadAlliedUnit(entity.owner(), transform)
        if (dead != null) setEntityTarget(target, dead)
        else assignSafeIdleNearCore(entity.owner(), target)
    }

    private fun bestMemoryNode(transform: Transform): Entity? {
        return world.entitiesWithComponent(ResourceNode::class.java)
            .filter { it.get(ResourceNode::class.java)?.nodeType == ResourceNodeType.MEMORY }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, t)
            }
    }

    private fun bestCpuNodeForWorker(owner: OwnerType, transform: Transform): Entity? {
        val playerIndex = owner.playerIndexOrNull()

        return world.entitiesWithComponent(ResourceNode::class.java)
            .filter { it.get(ResourceNode::class.java)?.nodeType == ResourceNodeType.CPU }
            .minByOrNull {
                val node = it.get(ResourceNode::class.java) ?: return@minByOrNull Float.MAX_VALUE
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                val currentSharePenalty = when (playerIndex) {
                    1 -> node.captureProgressPlayer1 * 180f
                    2 -> node.captureProgressPlayer2 * 180f
                    else -> 0f
                }
                GameMath.distance(transform, t) + currentSharePenalty
            }
    }


    private fun assignPatchHealerTarget(entity: Entity, target: Target, transform: Transform) {
        val ally = world.getAliveEntities()
            .filter { it.owner() == entity.owner() && it.id != entity.id && it.has(UnitComponent::class.java) }
            .mapNotNull {
                val h = it.get(Health::class.java) ?: return@mapNotNull null
                val t = it.get(Transform::class.java) ?: return@mapNotNull null
                if (h.current < h.max) it to GameMath.distance(transform, t) else null
            }
            .minByOrNull { it.second }
            ?.first

        if (ally != null) setEntityTarget(target, ally)
        else assignSafeIdleNearCore(entity.owner(), target)
    }

    private fun assignDefensivePosition(entity: Entity, target: Target, transform: Transform) {
        val enemyNear = nearestEnemyUnit(entity.owner().opponent(), transform, 260f)
        if (enemyNear != null) {
            setEntityTarget(target, enemyNear)
            return
        }

        val point = defensivePointFor(entity.owner(), entity.get(UnitComponent::class.java)?.typeName)
        target.targetEntityId = null
        target.targetX = point.first
        target.targetY = point.second
    }

    private fun assignStructureAttackTarget(entity: Entity, target: Target, transform: Transform) {
        val enemyStructure = world.getAliveEntities()
            .filter {
                it.owner().isPlayer() &&
                        it.owner() != entity.owner() &&
                        (it.has(Factory::class.java) || it.has(Core::class.java))
            }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                val base = if (it.has(Factory::class.java)) 0f else 120f
                base + GameMath.distance(transform, t)
            }

        if (enemyStructure != null) setEntityTarget(target, enemyStructure)
        else assignCombatTarget(entity, target, transform)
    }

    private fun assignCombatTarget(entity: Entity, target: Target, transform: Transform) {
        val unit = entity.get(UnitComponent::class.java) ?: return
        val enemy = world.getAliveEntities()
            .filter {
                it.owner().isPlayer() &&
                        it.owner() != entity.owner() &&
                        it.has(Health::class.java)
            }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                targetScore(unit, it, GameMath.distance(transform, t))
            }

        if (enemy != null) setEntityTarget(target, enemy)
        else {
            val enemyCore = findCore(entity.owner().opponent())
            if (enemyCore != null) setEntityTarget(target, enemyCore)
        }
    }

    private fun assignObserverPosition(entity: Entity, target: Target, transform: Transform, now: Long) {
        val enemy = nearestEnemyUnit(entity.owner().opponent(), transform, 520f)
        if (enemy != null) {
            val enemyTransform = enemy.get(Transform::class.java)
            if (enemyTransform != null) {
                target.targetEntityId = null
                val core = findCore(entity.owner())?.get(Transform::class.java)
                if (core != null) {
                    val dx = core.x - enemyTransform.x
                    val dy = core.y - enemyTransform.y
                    val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    target.targetX = enemyTransform.x + dx / len * 145f
                    target.targetY = enemyTransform.y + dy / len * 145f
                }
                markNearestEnemy(entity, entity.get(UnitComponent::class.java) ?: return, transform, now)
            }
        } else {
            assignSafeIdleNearCore(entity.owner(), target)
        }
    }

    private fun assignSafeIdleNearCore(owner: OwnerType, target: Target) {
        val core = findCore(owner)?.get(Transform::class.java)
        if (core != null) {
            val forward = if (owner == OwnerType.PLAYER_1) 90f else -90f
            target.targetEntityId = null
            target.targetX = core.x + 70f
            target.targetY = core.y + forward
        }
    }

    private fun bestNodeForAllocator(owner: OwnerType, transform: Transform, preferMemory: Boolean): Entity? {
        val playerIndex = owner.playerIndexOrNull()

        val memoryNodes = world.entitiesWithComponent(ResourceNode::class.java)
            .filter { it.get(ResourceNode::class.java)?.nodeType == ResourceNodeType.MEMORY }

        val cpuNodes = world.entitiesWithComponent(ResourceNode::class.java)
            .filter {
                val node = it.get(ResourceNode::class.java)
                node?.nodeType == ResourceNodeType.CPU && node.capturedBy != playerIndex
            }

        val candidates = if (preferMemory) memoryNodes + cpuNodes else cpuNodes + memoryNodes

        return candidates.minByOrNull {
            val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
            val node = it.get(ResourceNode::class.java)
            val priority = when {
                preferMemory && node?.nodeType == ResourceNodeType.MEMORY -> 0f
                !preferMemory && node?.nodeType == ResourceNodeType.CPU -> 0f
                else -> 240f
            }
            priority + GameMath.distance(transform, t)
        }
    }

    private fun defensivePointFor(owner: OwnerType, unitType: UnitType?): Pair<Float, Float> {
        val core = findCore(owner)?.get(Transform::class.java)
            ?: return GameConfig.worldWidth / 2f to GameConfig.worldHeight / 2f

        val forward = if (owner == OwnerType.PLAYER_1) 150f else -150f
        val side = when (unitType) {
            UnitType.FIREWALL -> 0f
            UnitType.MUTEX -> -70f
            UnitType.SEMAPHORE -> 70f
            UnitType.EXCEPTION_HANDLER -> 35f
            UnitType.HEAP_BLOCK -> -35f
            else -> 0f
        }

        return (core.x + side).coerceIn(40f, GameConfig.worldWidth - 40f) to
                (core.y + forward).coerceIn(40f, GameConfig.worldHeight - 40f)
    }

    private fun nearestEnemyUnit(enemyOwner: OwnerType, transform: Transform, radius: Float): Entity? {
        return world.getAliveEntities()
            .filter { it.owner() == enemyOwner && it.has(UnitComponent::class.java) }
            .filter {
                val t = it.get(Transform::class.java) ?: return@filter false
                GameMath.distance(transform, t) <= radius
            }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, t)
            }
    }

    private fun updateResourceIncome(now: Long) {
        if (now - lastIncomeAt < GameConfig.resourceIncomeIntervalMillis) return
        lastIncomeAt = now
        recalculateIncome()
        playerRuntimes.values.forEach { runtime ->
            runtime.cpu += runtime.cpuIncome
        }
    }

    private fun recalculateIncome() {
        playerRuntimes.values.forEach { runtime ->
            runtime.memoryIncome = 0
            runtime.cpuIncome = GameConfig.baseCpuIncome
        }

        world.entitiesWithComponent(Factory::class.java).forEach { factoryEntity ->
            val factory = factoryEntity.get(Factory::class.java) ?: return@forEach
            val health = factoryEntity.get(Health::class.java) ?: return@forEach
            if (health.isDead) return@forEach

            val playerIndex = factoryEntity.owner().playerIndexOrNull() ?: return@forEach
            val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == playerIndex } ?: return@forEach

            if (factory.factoryType == FactoryType.SUPPORT) runtime.cpuIncome += GameConfig.supportFactoryCpuIncomeBonus
        }

        world.entitiesWithComponent(ResourceNode::class.java).forEach { nodeEntity ->
            val node = nodeEntity.get(ResourceNode::class.java) ?: return@forEach
            if (node.nodeType != ResourceNodeType.CPU) return@forEach

            val p1Runtime = playerRuntimes.values.firstOrNull { it.playerIndex == 1 }
            val p2Runtime = playerRuntimes.values.firstOrNull { it.playerIndex == 2 }

            val p1Income = kotlin.math.floor(node.totalCpuShare * node.captureProgressPlayer1).toInt()
            val p2Income = kotlin.math.floor(node.totalCpuShare * node.captureProgressPlayer2).toInt()

            if (p1Runtime != null) p1Runtime.cpuIncome += p1Income
            if (p2Runtime != null) p2Runtime.cpuIncome += p2Income

            node.capturedBy = when {
                node.captureProgressPlayer1 > node.captureProgressPlayer2 -> 1
                node.captureProgressPlayer2 > node.captureProgressPlayer1 -> 2
                else -> null
            }
        }
    }

    private fun processFactoryQueues(now: Long) {
        factoryQueues.values.forEach { queueState ->
            val factoryEntity = world.getEntity(queueState.factoryId)
            val factoryAlive = factoryEntity?.get(Health::class.java)?.isDead == false
            if (!factoryAlive) {
                queueState.queue.clear()
                queueState.readyAt = 0L
                return@forEach
            }

            if (queueState.queue.isEmpty() || now < queueState.readyAt) return@forEach

            val order = queueState.queue.removeFirst()
            val spawnPoint = spawnPointNearFactory(factoryEntity!!, queueState.owner)
            val entity = UnitFactory.createUnit(
                world = world,
                unitType = order.unitType,
                owner = queueState.owner,
                x = spawnPoint.first,
                y = spawnPoint.second,
            )

            val target = entity.get(Target::class.java)
            val autoTargetEntity = chooseInitialAutoTargetEntity(entity, order.unitType)
            if (target != null && autoTargetEntity != null) setEntityTarget(target, autoTargetEntity)

            val playerId = playerIdByOwner(queueState.owner)
            if (playerId != null) inc(stats[playerId]?.unitsProduced, order.unitType)

            pushText(queueState.owner, spawnPoint.first, spawnPoint.second + 45f, "${UnitRegistry.getConfig(order.unitType).displayName} process started")

            queueState.readyAt = if (queueState.queue.isEmpty()) 0L else now + queueState.queue.first().buildMillis
        }
    }

    private fun chooseInitialAutoTarget(owner: OwnerType, unitType: UnitType, spawnX: Float, spawnY: Float): Pair<Float, Float> {
        val spawnTransform = Transform(spawnX, spawnY)
        val config = UnitRegistry.getConfig(unitType)

        return when {
            config.memoryWorkPower > 0f && config.cpuRedirectPower <= 0f -> {
                val node = bestMemoryNode(spawnTransform)
                val t = node?.get(Transform::class.java)
                if (t != null) t.x to t.y else spawnX to spawnY
            }

            config.cpuRedirectPower > 0f && config.memoryWorkPower <= 0f -> {
                val node = bestCpuNodeForWorker(owner, spawnTransform)
                val t = node?.get(Transform::class.java)
                if (t != null) t.x to t.y else spawnX to spawnY
            }

            config.cpuRedirectPower > 0f && config.memoryWorkPower > 0f -> {
                val node = bestMemoryNode(spawnTransform) ?: bestCpuNodeForWorker(owner, spawnTransform)
                val t = node?.get(Transform::class.java)
                if (t != null) t.x to t.y else spawnX to spawnY
            }

            unitType == UnitType.GARBAGE_COLLECTOR -> {
                val dead = nearestDeadAlliedUnit(owner, spawnTransform)
                val t = dead?.get(Transform::class.java)
                if (t != null) t.x to t.y else defensivePointFor(owner, unitType)
            }

            unitType == UnitType.THREAD_GUARD ||
                    unitType == UnitType.FIREWALL ||
                    unitType == UnitType.MUTEX ||
                    unitType == UnitType.SEMAPHORE ||
                    unitType == UnitType.EXCEPTION_HANDLER ||
                    unitType == UnitType.HEAP_BLOCK -> defensivePointFor(owner, unitType)

            else -> {
                val enemyCore = findCore(owner.opponent())?.get(Transform::class.java)
                if (enemyCore != null) enemyCore.x to enemyCore.y else spawnX to spawnY
            }
        }
    }

    private fun chooseInitialAutoTargetEntity(entity: Entity, unitType: UnitType): Entity? {
        val transform = entity.get(Transform::class.java) ?: return null
        val config = UnitRegistry.getConfig(unitType)

        return when {
            config.memoryWorkPower > 0f && config.cpuRedirectPower <= 0f -> bestMemoryNode(transform)
            config.cpuRedirectPower > 0f && config.memoryWorkPower <= 0f -> bestCpuNodeForWorker(entity.owner(), transform)
            config.cpuRedirectPower > 0f && config.memoryWorkPower > 0f -> bestMemoryNode(transform) ?: bestCpuNodeForWorker(entity.owner(), transform)
            unitType == UnitType.GARBAGE_COLLECTOR -> nearestDeadAlliedUnit(entity.owner(), transform)
            unitType == UnitType.INJECTOR -> world.getAliveEntities()
                .filter { it.owner() == entity.owner().opponent() && (it.has(Factory::class.java) || it.has(Core::class.java)) }
                .minByOrNull {
                    val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                    GameMath.distance(transform, t)
                }
            else -> null
        }
    }

    private fun updateMemoryWorkers(deltaSeconds: Float, now: Long) {
        world.getAliveEntities()
            .filter {
                val unit = it.get(UnitComponent::class.java) ?: return@filter false
                unit.memoryWorkPower > 0f
            }
            .forEach { worker ->
                val unit = worker.get(UnitComponent::class.java) ?: return@forEach
                val transform = worker.get(Transform::class.java) ?: return@forEach
                val process = worker.get(ProcessState::class.java) ?: return@forEach
                val targetNode = worker.get(Target::class.java)?.targetEntityId?.let { world.getEntity(it) }
                    ?: bestMemoryNode(transform)
                    ?: return@forEach

                val node = targetNode.get(ResourceNode::class.java) ?: return@forEach
                if (node.nodeType != ResourceNodeType.MEMORY) return@forEach

                val nodeTransform = targetNode.get(Transform::class.java) ?: return@forEach
                if (GameMath.distance(transform, nodeTransform) > node.captureRadius) return@forEach

                if (process.phaseStartedAt <= 0L || process.lastEvent != "MEMORY_WORK") {
                    process.phaseStartedAt = now
                    process.internalCounter = 0
                    process.lastEvent = "MEMORY_WORK"
                    pushText(worker.owner(), transform.x, transform.y + 42f, memoryWorkStartText(unit.typeName))
                }

                process.internalCounter += (unit.memoryWorkPower * deltaSeconds).toInt().coerceAtLeast(1)

                if (process.internalCounter < GameConfig.memoryWorkRequired) return@forEach

                val playerIndex = worker.owner().playerIndexOrNull() ?: return@forEach
                val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == playerIndex } ?: return@forEach
                val batch = memoryBatchFor(unit.typeName)

                runtime.memory += batch
                runtime.memoryAllocatedTotal += batch
                stats[runtime.playerId]?.memoryAllocated = stats[runtime.playerId]?.memoryAllocated?.plus(batch) ?: batch

                pushText(worker.owner(), transform.x, transform.y + 42f, "+$batch Memory allocated")
                completeAndRemoveProcess(worker, "${UnitRegistry.getConfig(unit.typeName).displayName} completed")
            }
    }

    private fun memoryBatchFor(unitType: UnitType): Int {
        return when (unitType) {
            UnitType.ALLOCATOR -> GameConfig.allocatorMemoryBatch
            UnitType.BUFFER -> GameConfig.bufferMemoryBatch
            UnitType.MEMORY_POOL -> GameConfig.memoryPoolBatch
            UnitType.DMA_CONTROLLER -> GameConfig.dmaMemoryBatch
            else -> GameConfig.allocatorMemoryBatch
        }
    }

    private fun memoryWorkStartText(unitType: UnitType): String {
        return when (unitType) {
            UnitType.ALLOCATOR -> "allocating memory..."
            UnitType.BUFFER -> "buffering memory chunk..."
            UnitType.MEMORY_POOL -> "preallocating pool..."
            UnitType.DMA_CONTROLLER -> "DMA transfer into memory..."
            else -> "allocating memory..."
        }
    }


    private fun updateGarbageCollectorWork(now: Long) {
        world.getAliveEntities()
            .filter { it.get(UnitComponent::class.java)?.typeName == UnitType.GARBAGE_COLLECTOR }
            .forEach { collector ->
                val transform = collector.get(Transform::class.java) ?: return@forEach
                val process = collector.get(ProcessState::class.java) ?: return@forEach
                val dead = nearestDeadAlliedUnit(collector.owner(), transform)

                if (dead == null) {
                    process.lastEvent = "waiting for garbage"
                    return@forEach
                }

                val deadTransform = dead.get(Transform::class.java) ?: return@forEach
                val distance = GameMath.distance(transform, deadTransform)
                if (distance > 70f) {
                    val target = collector.get(Target::class.java) ?: return@forEach
                    target.targetEntityId = null
                    target.targetX = deadTransform.x
                    target.targetY = deadTransform.y
                    return@forEach
                }

                if (process.phase != ProcessPhase.GARBAGE_COLLECTING) {
                    process.phase = ProcessPhase.GARBAGE_COLLECTING
                    process.phaseStartedAt = now
                    process.lastEvent = "mark/sweep"
                    pushText(collector.owner(), transform.x, transform.y + 42f, "marking unreachable object...")
                }

                if (now - process.phaseStartedAt < GameConfig.garbageCollectorWorkMillis) return@forEach

                val freed = dead.get(UnitComponent::class.java)?.allocatedMemory ?: 0
                val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == collector.owner().playerIndexOrNull() }
                if (runtime != null) {
                    runtime.memory += freed
                    runtime.memoryFreedTotal += freed
                    stats[runtime.playerId]?.memoryFreed = stats[runtime.playerId]?.memoryFreed?.plus(freed) ?: freed
                }

                world.removeEntity(dead.id)
                pushText(collector.owner(), transform.x, transform.y + 42f, "swept garbage: +$freed Memory")
                completeAndRemoveProcess(collector, "Garbage Collector finished sweep")
            }
    }

    private fun updateSpecialProcesses(now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
            .forEach { entity ->
                val unit = entity.get(UnitComponent::class.java) ?: return@forEach
                val process = entity.get(ProcessState::class.java) ?: return@forEach
                val transform = entity.get(Transform::class.java) ?: return@forEach

                when (unit.typeName) {
                    UnitType.STACK_FRAME -> {
                        if (process.internalCounter <= 0 && now - process.phaseStartedAt > 1600L) {
                            completeAndRemoveProcess(entity, "Stack frame returned")
                        }
                    }

                    UnitType.RECURSIVE_CALL -> {
                        if (process.nextActionAt <= 0L) process.nextActionAt = now + 2200L
                        if (now >= process.nextActionAt) {
                            process.internalCounter += 1
                            process.nextActionAt = now + 2200L
                            pushText(entity.owner(), transform.x, transform.y + 42f, "recursive depth ${process.internalCounter}")
                            if (process.internalCounter >= 5) {
                                entity.get(Health::class.java)?.damage(18)
                                pushText(entity.owner(), transform.x, transform.y + 42f, "stack overflow risk")
                            }
                        }
                    }

                    UnitType.LOOP -> {
                        if (process.nextActionAt <= 0L) process.nextActionAt = now + 900L
                        if (now >= process.nextActionAt) {
                            process.nextActionAt = now + 900L
                            pushText(entity.owner(), transform.x, transform.y + 42f, "loop iteration")
                        }
                    }

                    UnitType.POINTER,
                    UnitType.OBSERVER -> {
                        if (process.nextActionAt <= 0L) process.nextActionAt = now + 1200L
                        if (now >= process.nextActionAt) {
                            process.nextActionAt = now + 1200L
                            markNearestEnemy(entity, unit, transform, now)
                        }
                    }

                    else -> {}
                }
            }
    }

    private fun updateAuras(now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
            .forEach { entity ->
                val unit = entity.get(UnitComponent::class.java) ?: return@forEach
                val transform = entity.get(Transform::class.java) ?: return@forEach

                when (unit.typeName) {
                    UnitType.MUTEX -> {
                        applyAllyAura(entity.owner(), transform, 125f) { ally, allyTransform ->
                            ally.get(StatusEffects::class.java)?.protectedUntil = now + 350L
                            if ((now / 1000L) % 3L == 0L) {
                                pushText(entity.owner(), allyTransform.x, allyTransform.y + 42f, "mutex protected critical section")
                            }
                        }
                    }

                    UnitType.SEMAPHORE -> {
                        applyAllyAura(entity.owner(), transform, 135f) { ally, allyTransform ->
                            ally.get(StatusEffects::class.java)?.throughputUntil = now + 350L
                            if ((now / 1000L) % 3L == 0L) {
                                pushText(entity.owner(), allyTransform.x, allyTransform.y + 42f, "semaphore permits throughput")
                            }
                        }
                    }

                    UnitType.EXCEPTION_HANDLER -> {
                        applyAllyAura(entity.owner(), transform, 120f) { ally, allyTransform ->
                            val effects = ally.get(StatusEffects::class.java) ?: return@applyAllyAura
                            if (!effects.exceptionShieldUsed && (now / 1000L) % 4L == 0L) {
                                pushText(entity.owner(), allyTransform.x, allyTransform.y + 42f, "exception handler ready")
                            }
                        }
                    }

                    else -> {}
                }
            }
    }

    private fun applyAllyAura(owner: OwnerType, source: Transform, radius: Float, effect: (Entity, Transform) -> Unit) {
        world.getAliveEntities()
            .filter { it.owner() == owner && it.has(UnitComponent::class.java) }
            .forEach { ally ->
                val t = ally.get(Transform::class.java) ?: return@forEach
                if (GameMath.distance(source, t) <= radius) effect(ally, t)
            }
    }

    private fun updateNodeCapture(deltaSeconds: Float) {
        val units = world.getAliveEntities()
            .filter { it.get(UnitComponent::class.java)?.typeName == UnitType.CACHE_RUNNER }

        world.entitiesWithComponent(ResourceNode::class.java)
            .filter { it.get(ResourceNode::class.java)?.nodeType == ResourceNodeType.CPU }
            .forEach { nodeEntity ->
                val nodeTransform = nodeEntity.get(Transform::class.java) ?: return@forEach
                val node = nodeEntity.get(ResourceNode::class.java) ?: return@forEach

                val p1 = units.count { it.owner() == OwnerType.PLAYER_1 && isInside(it, nodeTransform, node.captureRadius) }
                val p2 = units.count { it.owner() == OwnerType.PLAYER_2 && isInside(it, nodeTransform, node.captureRadius) }
                val speed = 0.25f * deltaSeconds

                when {
                    p1 > 0 && p2 == 0 -> {
                        node.captureProgressPlayer1 = (node.captureProgressPlayer1 + speed * p1).coerceAtMost(1f)
                        node.captureProgressPlayer2 = (node.captureProgressPlayer2 - speed).coerceAtLeast(0f)
                        if (node.captureProgressPlayer1 >= 1f) node.capturedBy = 1
                    }

                    p2 > 0 && p1 == 0 -> {
                        node.captureProgressPlayer2 = (node.captureProgressPlayer2 + speed * p2).coerceAtMost(1f)
                        node.captureProgressPlayer1 = (node.captureProgressPlayer1 - speed).coerceAtLeast(0f)
                        if (node.captureProgressPlayer2 >= 1f) node.capturedBy = 2
                    }
                }
            }
    }

    private fun updateUnitTargets() {
        val alive = world.getAliveEntities()
        val units = alive.filter { it.has(UnitComponent::class.java) }

        units.forEach { unit ->
            val unitComponent = unit.get(UnitComponent::class.java) ?: return@forEach
            if (unitComponent.typeName == UnitType.ALLOCATOR || unitComponent.typeName == UnitType.GARBAGE_COLLECTOR || unitComponent.typeName == UnitType.PATCH_HEALER) return@forEach

            val target = unit.get(Target::class.java) ?: return@forEach
            val unitTransform = unit.get(Transform::class.java) ?: return@forEach
            val combat = unit.get(CombatStats::class.java) ?: return@forEach

            val currentTarget = target.targetEntityId?.let { world.getEntity(it) }
            val currentTargetAlive = currentTarget?.get(Health::class.java)?.isDead == false

            val enemy = alive
                .filter { candidate ->
                    candidate.owner().isPlayer() &&
                            candidate.owner() != unit.owner() &&
                            candidate.has(Health::class.java) &&
                            isValidTargetFor(unitComponent, unit, unitTransform, combat, candidate)
                }
                .minByOrNull { candidate ->
                    val ct = candidate.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                    targetScore(unitComponent, candidate, GameMath.distance(unitTransform, ct))
                }

            if (enemy != null && (!currentTargetAlive || shouldReplaceTarget(unitComponent, unitTransform, combat, currentTarget, enemy))) {
                setEntityTarget(target, enemy)
            } else if (!currentTargetAlive) {
                restoreRallyTarget(unit, target, unitTransform)
            }
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

                val speedMultiplier = when {
                    effects?.isOverclocked(now) == true -> 1.45f
                    effects?.hasThroughputBoost(now) == true -> 1.22f
                    else -> 1f
                }

                val targetEntity = target.targetEntityId?.let { world.getEntity(it) }

                if (targetEntity != null) {
                    val targetTransform = targetEntity.get(Transform::class.java) ?: return@forEach
                    val distance = GameMath.distance(transform, targetTransform)

                    if (distance > combat.attackRange * 0.85f) {
                        GameMath.moveTowards(transform, targetTransform.x, targetTransform.y, combat.moveSpeed * speedMultiplier, deltaSeconds)
                    }
                } else {
                    val tx = target.targetX ?: return@forEach
                    val ty = target.targetY ?: return@forEach
                    GameMath.moveTowards(transform, tx, ty, combat.moveSpeed * speedMultiplier, deltaSeconds)
                }
            }
    }

    private fun updateCombat(now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
            .forEach { entity ->
                val unit = entity.get(UnitComponent::class.java) ?: return@forEach
                if (
                    unit.typeName == UnitType.ALLOCATOR ||
                    unit.typeName == UnitType.GARBAGE_COLLECTOR ||
                    unit.typeName == UnitType.PATCH_HEALER ||
                    unit.typeName == UnitType.MUTEX ||
                    unit.typeName == UnitType.SEMAPHORE ||
                    unit.typeName == UnitType.EXCEPTION_HANDLER ||
                    unit.typeName == UnitType.POINTER ||
                    unit.typeName == UnitType.OBSERVER
                ) return@forEach

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

                val cooldown = if (effects?.isOverclocked(now) == true) (combat.attackCooldownMillis * 0.7f).toLong() else combat.attackCooldownMillis
                if (now - combat.lastAttackAt < cooldown) return@forEach

                combat.lastAttackAt = now

                var finalDamage = combat.damage
                val targetEffects = targetEntity.get(StatusEffects::class.java)
                if (targetEffects?.isMarked(now) == true) finalDamage = (finalDamage * 1.28f).toInt().coerceAtLeast(finalDamage + 1)
                if (targetEffects?.isProtected(now) == true) finalDamage = (finalDamage * 0.70f).toInt().coerceAtLeast(1)

                val fatal = targetHealth.current - finalDamage <= 0
                if (fatal && tryExceptionShield(targetEntity, now)) {
                    targetHealth.current = 1
                    val tt = targetEntity.get(Transform::class.java)
                    if (tt != null) pushText(targetEntity.owner(), tt.x, tt.y + 42f, "exception caught")
                    return@forEach
                }

                targetHealth.damage(finalDamage)

                if (unit.typeName == UnitType.STACK_FRAME) {
                    entity.get(ProcessState::class.java)?.internalCounter = 1
                    completeAndRemoveProcess(entity, "Stack frame returned after call")
                }

                val targetUnit = targetEntity.get(UnitComponent::class.java)
                if (targetHealth.isDead && targetUnit != null) {
                    val killerId = playerIdByOwner(entity.owner())
                    val victimId = playerIdByOwner(targetEntity.owner())
                    if (killerId != null) inc(stats[killerId]?.enemyUnitsKilled, targetUnit.typeName)
                    if (victimId != null) inc(stats[victimId]?.unitsLost, targetUnit.typeName)
                    markDeadProcess(targetEntity, now, "process crashed; memory still allocated")
                    pushText(targetEntity.owner(), targetTransform.x, targetTransform.y + 42f, "dead object retained in memory")
                }
            }
    }

    private fun updatePatchHealer(now: Long) {
        world.getAliveEntities()
            .filter { it.get(UnitComponent::class.java)?.typeName == UnitType.PATCH_HEALER }
            .forEach { healer ->
                val transform = healer.get(Transform::class.java) ?: return@forEach
                val combat = healer.get(CombatStats::class.java) ?: return@forEach
                if (now - combat.lastAttackAt < 1200L) return@forEach

                val ally = world.getAliveEntities()
                    .filter { it.owner() == healer.owner() && it.id != healer.id && it.has(Health::class.java) }
                    .mapNotNull { candidate ->
                        val t = candidate.get(Transform::class.java) ?: return@mapNotNull null
                        val h = candidate.get(Health::class.java) ?: return@mapNotNull null
                        if (h.current < h.max) candidate to GameMath.distance(transform, t) else null
                    }
                    .minByOrNull { it.second }
                    ?.first

                if (ally != null) {
                    val allyTransform = ally.get(Transform::class.java)
                    if (allyTransform != null && GameMath.distance(transform, allyTransform) > 115f) {
                        healer.get(Target::class.java)?.apply {
                            targetEntityId = ally.id
                            targetX = allyTransform.x
                            targetY = allyTransform.y
                        }
                        return@forEach
                    }

                    val health = ally.get(Health::class.java) ?: return@forEach
                    combat.lastAttackAt = now
                    health.heal(14)
                    pushText(healer.owner(), transform.x, transform.y + 42f, "patched live process")
                }
            }
    }

    private fun updateDeaths(now: Long) {
        world.getEntities().forEach { entity ->
            val health = entity.get(Health::class.java) ?: return@forEach
            val unit = entity.get(UnitComponent::class.java) ?: return@forEach
            val process = entity.get(ProcessState::class.java) ?: return@forEach
            if (health.isDead && process.phase == ProcessPhase.RUNNING) {
                markDeadProcess(entity, now, "process crashed; awaiting GC")
                val victimId = playerIdByOwner(entity.owner())
                if (victimId != null) inc(stats[victimId]?.unitsLost, unit.typeName)
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
        finishGame(core.playerIndex, "Core destroyed")
    }

    private fun castDeadlock(owner: OwnerType, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val radius = UnitRegistry.getConfig(UnitType.DEADLOCK).attackRange
        world.getAliveEntities()
            .filter { it.owner().isPlayer() && it.owner() != owner && it.has(StatusEffects::class.java) }
            .forEach { enemy ->
                val transform = enemy.get(Transform::class.java) ?: return@forEach
                val effects = enemy.get(StatusEffects::class.java) ?: return@forEach
                if (GameMath.distance(transform.x, transform.y, x, y) <= radius) {
                    effects.stunnedUntil = now + 2500L
                    pushText(enemy.owner(), transform.x, transform.y + 42f, "deadlocked: waiting forever")
                }
            }

        scope.launch {
            GameDispatcher.sendToAllPlayers(players, SystemMessageEvent("Deadlock blocked enemy execution flow"))
        }
    }

    private fun castOverclock(owner: OwnerType, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val radius = UnitRegistry.getConfig(UnitType.OVERCLOCK).attackRange
        world.getAliveEntities()
            .filter { it.owner() == owner && it.has(StatusEffects::class.java) }
            .forEach { ally ->
                val transform = ally.get(Transform::class.java) ?: return@forEach
                val effects = ally.get(StatusEffects::class.java) ?: return@forEach
                if (GameMath.distance(transform.x, transform.y, x, y) <= radius) {
                    effects.overclockUntil = now + 4500L
                    pushText(owner, transform.x, transform.y + 42f, "overclocked throughput")
                }
            }

        scope.launch {
            GameDispatcher.sendToAllPlayers(players, SystemMessageEvent("Overclock boosted allied process throughput"))
        }
    }

    private fun castNullPointer(owner: OwnerType, x: Float, y: Float) {
        val now = System.currentTimeMillis()
        val config = UnitRegistry.getConfig(UnitType.NULL_POINTER)

        val enemy = world.getAliveEntities()
            .filter { it.owner().isPlayer() && it.owner() != owner && it.has(UnitComponent::class.java) }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(t.x, t.y, x, y)
            } ?: return

        val transform = enemy.get(Transform::class.java) ?: return
        if (GameMath.distance(transform.x, transform.y, x, y) > config.attackRange) return

        enemy.get(StatusEffects::class.java)?.stunnedUntil = now + 1600L
        enemy.get(Health::class.java)?.damage(config.damage)
        pushText(enemy.owner(), transform.x, transform.y + 42f, "NullPointerException")
    }

    private fun tryExceptionShield(target: Entity, now: Long): Boolean {
        val targetTransform = target.get(Transform::class.java) ?: return false
        val handler = world.getAliveEntities()
            .filter { it.owner() == target.owner() && it.get(UnitComponent::class.java)?.typeName == UnitType.EXCEPTION_HANDLER }
            .firstOrNull { candidate ->
                val effects = candidate.get(StatusEffects::class.java) ?: return@firstOrNull false
                if (effects.exceptionShieldUsed) return@firstOrNull false
                val t = candidate.get(Transform::class.java) ?: return@firstOrNull false
                GameMath.distance(t, targetTransform) <= 135f
            } ?: return false

        handler.get(StatusEffects::class.java)?.exceptionShieldUsed = true
        val ht = handler.get(Transform::class.java)
        if (ht != null) pushText(handler.owner(), ht.x, ht.y + 42f, "caught exception")
        return true
    }

    private fun markNearestEnemy(source: Entity, unit: UnitComponent, transform: Transform, now: Long) {
        val range = if (unit.typeName == UnitType.OBSERVER) 190f else 160f
        val enemy = world.getAliveEntities()
            .filter { it.owner().isPlayer() && it.owner() != source.owner() && it.has(UnitComponent::class.java) }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, t)
            } ?: return

        val enemyTransform = enemy.get(Transform::class.java) ?: return
        if (GameMath.distance(transform, enemyTransform) > range) return

        enemy.get(StatusEffects::class.java)?.markedUntil = now + 2200L
        pushText(source.owner(), enemyTransform.x, enemyTransform.y + 42f, if (unit.typeName == UnitType.OBSERVER) "observed: weak reference tracked" else "pointer referenced target")
    }

    private fun finishGame(loserPlayerIndex: Int, reason: String) {
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
                    reason = reason,
                    stats = statsSnapshot()
                )
            )
            delay(1000L)
            stop()
            onRoomFinished(roomId)
        }
    }

    private fun completeAndRemoveProcess(entity: Entity, message: String) {
        val transform = entity.get(Transform::class.java)
        if (transform != null) pushText(entity.owner(), transform.x, transform.y + 42f, message)
        world.removeEntity(entity.id)
    }

    private fun markDeadProcess(entity: Entity, now: Long, message: String) {
        val process = entity.get(ProcessState::class.java) ?: return
        process.phase = ProcessPhase.DEAD
        process.completedAt = now
        process.lastEvent = message
    }

    private fun nearestNode(transform: Transform, owner: OwnerType): Entity? {
        return world.entitiesWithComponent(ResourceNode::class.java)
            .minByOrNull { nodeEntity ->
                val nodeTransform = nodeEntity.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, nodeTransform)
            }
    }

    private fun nearestDeadAlliedUnit(owner: OwnerType, transform: Transform): Entity? {
        return world.getDeadEntities()
            .filter { it.owner() == owner && it.has(UnitComponent::class.java) }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, t)
            }
    }

    private fun findCore(owner: OwnerType): Entity? {
        return world.entitiesWithComponent(Core::class.java).firstOrNull { it.owner() == owner }
    }

    private fun nextFactoryPosition(owner: OwnerType, type: FactoryType): Pair<Float, Float> {
        val core = findCore(owner)
        val t = core?.get(Transform::class.java)
        val cx = t?.x ?: GameConfig.worldWidth / 2f
        val cy = t?.y ?: GameConfig.worldHeight / 2f
        val count = world.entitiesWithComponent(Factory::class.java).count { it.owner() == owner }
        val angleBase = if (owner == OwnerType.PLAYER_1) 0.35f else 3.49f
        val angle = angleBase + count * 0.55f + if (type == FactoryType.SUPPORT) 0.25f else 0f
        val radius = 135f + count * 22f
        val x = (cx + cos(angle) * radius).coerceIn(60f, GameConfig.worldWidth - 60f)
        val y = (cy + sin(angle) * radius).coerceIn(60f, GameConfig.worldHeight - 60f)
        return x to y
    }

    private fun recalculateFactoryMultipliers(owner: OwnerType, type: FactoryType) {
        val aliveFactories = world.getAliveEntities()
            .filter { it.owner() == owner && it.get(Factory::class.java)?.factoryType == type }
            .mapNotNull { it.get(Factory::class.java) }

        val multiplier = 1f + (aliveFactories.size - 1).coerceAtLeast(0) * GameConfig.factoryProductionMultiplierBonus
        aliveFactories.forEach { it.productionMultiplier = multiplier }
    }

    private fun queueLimit(owner: OwnerType, type: FactoryType): Int {
        return world.getAliveEntities()
            .count { it.owner() == owner && it.get(Factory::class.java)?.factoryType == type }
            .coerceAtLeast(1) * 3
    }

    private fun findFactoryQueue(owner: OwnerType, type: FactoryType): FactoryQueueState? {
        return factoryQueues.values
            .filter { it.owner == owner && it.type == type }
            .filter {
                val entity = world.getEntity(it.factoryId) ?: return@filter false
                val health = entity.get(Health::class.java) ?: return@filter false
                !health.isDead
            }
            .minByOrNull { it.queue.size }
    }

    private fun playerIdByOwner(owner: OwnerType): Int? {
        val index = owner.playerIndexOrNull() ?: return null
        return playerRuntimes.values.firstOrNull { it.playerIndex == index }?.playerId
    }

    private fun statsSnapshot(): MatchStats {
        return MatchStats(stats.mapValues { it.value.snapshot() })
    }

    private fun inc(map: MutableMap<UnitType, Int>?, unitType: UnitType) {
        if (map == null) return
        map[unitType] = map.getOrDefault(unitType, 0) + 1
    }

    private fun pushText(owner: OwnerType, x: Float, y: Float, text: String) {
        textEvents.addLast(WorldTextEvent(textEventId++, owner, x, y, text, System.currentTimeMillis()))
        while (textEvents.size > 40) textEvents.removeFirst()
    }

    private fun cleanupTextEvents(now: Long) {
        while (textEvents.firstOrNull()?.let { now - it.createdAt > it.ttlMillis } == true) {
            textEvents.removeFirst()
        }
    }

    private fun factoryQueueSnapshot(): Map<Int, Map<FactoryType, Int>> {
        return playerRuntimes.values.associate { runtime ->
            val owner = OwnerType.fromPlayerIndex(runtime.playerIndex)
            val byFactory = FactoryType.entries.associateWith { factoryType ->
                factoryQueues.values
                    .filter { it.owner == owner && it.type == factoryType }
                    .sumOf { it.queue.size }
            }
            runtime.playerId to byFactory
        }
    }

    private fun isManualTargetCard(unitType: UnitType): Boolean {
        return unitType == UnitType.DEADLOCK || unitType == UnitType.OVERCLOCK || unitType == UnitType.NULL_POINTER
    }

    private fun requiredFactoryFor(unitType: UnitType): FactoryType {
        return when (unitType) {
            UnitType.ALLOCATOR,
            UnitType.BUFFER,
            UnitType.MEMORY_POOL,
            UnitType.DMA_CONTROLLER,
            UnitType.CPU_SCHEDULER,
            UnitType.LOAD_BALANCER,
            UnitType.INTERRUPT_HANDLER,
            UnitType.INJECTOR,
            UnitType.CACHE_RUNNER,
            UnitType.COROUTINE_ARCHER,
            UnitType.STACK_FRAME,
            UnitType.HEAP_BLOCK,
            UnitType.POINTER,
            UnitType.LOOP,
            UnitType.RECURSIVE_CALL -> FactoryType.BASIC

            UnitType.GARBAGE_COLLECTOR,
            UnitType.THREAD_GUARD,
            UnitType.FIREWALL,
            UnitType.PATCH_HEALER,
            UnitType.EXCEPTION_HANDLER,
            UnitType.MUTEX,
            UnitType.SEMAPHORE,
            UnitType.OBSERVER,
            UnitType.DEADLOCK,
            UnitType.OVERCLOCK,
            UnitType.NULL_POINTER -> FactoryType.SUPPORT
        }
    }

    private fun cooldownMillisFor(unitType: UnitType): Long {
        return when (unitType) {
            UnitType.ALLOCATOR -> 2200L
            UnitType.BUFFER -> 3200L
            UnitType.MEMORY_POOL -> 5200L
            UnitType.DMA_CONTROLLER -> 4200L
            UnitType.CPU_SCHEDULER -> 2800L
            UnitType.LOAD_BALANCER -> 5600L
            UnitType.INTERRUPT_HANDLER -> 3000L
            UnitType.CACHE_RUNNER -> 3000L
            UnitType.GARBAGE_COLLECTOR -> 5200L
            UnitType.PATCH_HEALER -> 4500L
            UnitType.THREAD_GUARD -> 6300L
            UnitType.FIREWALL -> 7600L
            UnitType.INJECTOR -> 6800L
            UnitType.COROUTINE_ARCHER -> 6400L
            UnitType.DEADLOCK -> 10000L
            UnitType.OVERCLOCK -> 8500L
            UnitType.STACK_FRAME -> 2800L
            UnitType.HEAP_BLOCK -> 4000L
            UnitType.POINTER -> 4300L
            UnitType.NULL_POINTER -> 9000L
            UnitType.EXCEPTION_HANDLER -> 8000L
            UnitType.LOOP -> 5000L
            UnitType.RECURSIVE_CALL -> 7800L
            UnitType.MUTEX -> 7200L
            UnitType.SEMAPHORE -> 7000L
            UnitType.OBSERVER -> 5200L
        }
    }

    private fun buildMillisFor(unitType: UnitType, queueState: FactoryQueueState): Long {
        val config = UnitRegistry.getConfig(unitType)
        val multiplier = world.getEntity(queueState.factoryId)?.get(Factory::class.java)?.productionMultiplier ?: 1f
        return ceil(((config.buildTime * 1000f) / multiplier.coerceAtLeast(0.1f)).toDouble()).toLong().coerceAtLeast(700L)
    }

    private fun spawnPointNearFactory(factory: Entity, owner: OwnerType): Pair<Float, Float> {
        val transform = factory.get(Transform::class.java) ?: return GameConfig.worldWidth / 2f to GameConfig.worldHeight / 2f
        val lateral = ((tick % 5L) - 2L).toFloat() * 12f
        val forward = if (owner == OwnerType.PLAYER_1) 58f else -58f
        val x = (transform.x + lateral).coerceIn(16f, GameConfig.worldWidth - 16f)
        val y = (transform.y + forward).coerceIn(16f, GameConfig.worldHeight - 16f)
        return x to y
    }

    private fun setEntityTarget(target: Target, enemy: Entity) {
        val enemyTransform = enemy.get(Transform::class.java) ?: return
        target.targetEntityId = enemy.id
        target.targetX = enemyTransform.x
        target.targetY = enemyTransform.y
    }

    private fun shouldReplaceTarget(attacker: UnitComponent, unitTransform: Transform, combat: CombatStats, currentTarget: Entity?, newTarget: Entity): Boolean {
        if (currentTarget == null) return true
        val currentTransform = currentTarget.get(Transform::class.java) ?: return true
        val newTransform = newTarget.get(Transform::class.java) ?: return false
        val currentScore = targetScore(attacker, currentTarget, GameMath.distance(unitTransform, currentTransform))
        val newScore = targetScore(attacker, newTarget, GameMath.distance(unitTransform, newTransform))
        return newScore + 12f < currentScore
    }

    private fun restoreRallyTarget(unit: Entity, target: Target, unitTransform: Transform) {
        val behavior: Behavior? = unit.get(CaptureBehavior::class.java)
            ?: unit.get(SupportBehavior::class.java)
            ?: unit.get(DefenseBehavior::class.java)
            ?: unit.get(AttackBehavior::class.java)

        target.targetEntityId = null
        target.targetX = behavior?.targetX ?: unitTransform.x
        target.targetY = behavior?.targetY ?: unitTransform.y
    }

    private fun isValidTargetFor(attacker: UnitComponent, source: Entity, sourceTransform: Transform, combat: CombatStats, candidate: Entity): Boolean {
        val candidateTransform = candidate.get(Transform::class.java) ?: return false
        val distance = GameMath.distance(sourceTransform, candidateTransform)

        return when (attacker.typeName) {
            UnitType.THREAD_GUARD,
            UnitType.FIREWALL -> candidate.has(UnitComponent::class.java) && distance <= 260f

            UnitType.INJECTOR -> candidate.has(Factory::class.java) || candidate.has(Core::class.java) || candidate.has(UnitComponent::class.java)
            UnitType.COROUTINE_ARCHER -> candidate.has(UnitComponent::class.java) || candidate.has(Factory::class.java) || candidate.has(Core::class.java)
            UnitType.CACHE_RUNNER -> candidate.has(UnitComponent::class.java) && distance <= combat.attackRange * 1.65f
            UnitType.STACK_FRAME,
            UnitType.LOOP,
            UnitType.RECURSIVE_CALL -> candidate.has(UnitComponent::class.java)
            UnitType.HEAP_BLOCK -> candidate.has(UnitComponent::class.java) && distance <= 120f
            else -> false
        }
    }

    private fun targetScore(attacker: UnitComponent, candidate: Entity, distance: Float): Float {
        val base = when (attacker.typeName) {
            UnitType.THREAD_GUARD,
            UnitType.FIREWALL -> if (candidate.has(UnitComponent::class.java)) 10f else Float.MAX_VALUE

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

            UnitType.CACHE_RUNNER -> if (candidate.has(UnitComponent::class.java)) 20f else Float.MAX_VALUE
            UnitType.STACK_FRAME -> if (candidate.has(UnitComponent::class.java)) 12f else Float.MAX_VALUE
            UnitType.HEAP_BLOCK -> if (candidate.has(UnitComponent::class.java)) 30f else Float.MAX_VALUE
            UnitType.LOOP -> if (candidate.has(UnitComponent::class.java)) 16f else Float.MAX_VALUE
            UnitType.RECURSIVE_CALL -> if (candidate.has(UnitComponent::class.java)) 14f else Float.MAX_VALUE
            else -> Float.MAX_VALUE
        }
        return base + distance / 10f
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
