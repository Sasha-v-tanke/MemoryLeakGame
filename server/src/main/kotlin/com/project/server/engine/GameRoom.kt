package com.project.server.engine

import com.project.server.engine.units.UnitConfig
import com.project.server.engine.units.UnitTargeting
import com.project.server.engine.units.UnitCombat
import com.project.server.engine.units.UnitSpells
import com.project.server.engine.units.UnitSpecialBehaviors
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

    // Unit systems
    private val unitTargeting = UnitTargeting(world)
    private val unitCombat = UnitCombat(world)
    private val unitSpells = UnitSpells(world)
    private val specialBehaviors = UnitSpecialBehaviors(world)

    private val specialBehaviorsCallback = object : UnitSpecialBehaviors.TextEventCallback {
        override fun pushText(owner: OwnerType, x: Float, y: Float, text: String) {
            this@GameRoom.pushText(owner, x, y, text)
        }
    }

    private val combatCallback = object : UnitCombat.CombatCallback {
        override fun pushText(owner: OwnerType, x: Float, y: Float, text: String) {
            this@GameRoom.pushText(owner, x, y, text)
        }

        override fun onUnitKilled(attacker: Entity, victim: Entity, victimType: UnitType) {
            val killerId = playerIdByOwner(attacker.owner())
            val victimId = playerIdByOwner(victim.owner())
            if (killerId != null) inc(stats[killerId]?.enemyUnitsKilled, victimType)
            if (victimId != null) inc(stats[victimId]?.unitsLost, victimType)
        }

        override fun markDeadProcess(entity: Entity, now: Long, message: String) {
            this@GameRoom.markDeadProcess(entity, now, message)
        }
    }

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
        if (!started.get()) return PlayCardResponse(false, "Игра ещё не началась")
        if (finished.get()) return PlayCardResponse(false, "Игра уже закончена")

        val runtime = playerRuntimes[request.playerId]
            ?: return PlayCardResponse(false, "Игрок не принадлежит этой комнате")

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
            return PlayCardResponse(false, "${config.displayName} перезарядка: ${"%.1f".format(seconds)}с")
        }

        val isManualCard = UnitConfig.isManualTargetCard(request.unitType)

        val spawnFactory: FactoryQueueState? = when {
            isManualCard -> null
            else -> {
                val type = UnitConfig.requiredFactoryFor(request.unitType)
                findFactoryQueue(owner, type)
                    ?: return PlayCardResponse(false, "Требуется ${type.name.lowercase().replaceFirstChar { it.uppercase() }} Factory")
            }
        }

        if (runtime.memory < config.costMemory) return PlayCardResponse(false, "Недостаточно выделенной памяти")
        if (runtime.cpu < config.costCpu) return PlayCardResponse(false, "Недостаточно CPU")

        runtime.memory -= config.costMemory
        runtime.cpu -= config.costCpu
        playerCardCooldowns.getOrPut(request.playerId) { mutableMapOf() }[request.unitType] = now + UnitConfig.cooldownMillisFor(request.unitType)

        when (request.unitType) {
            UnitType.DEADLOCK -> {
                inc(stats[request.playerId]?.spellsCast, request.unitType)
                castDeadlock(owner, targetX, targetY)
                return PlayCardResponse(true, "Deadlock применён в выбранной области")
            }

            UnitType.OVERCLOCK -> {
                inc(stats[request.playerId]?.spellsCast, request.unitType)
                castOverclock(owner, targetX, targetY)
                return PlayCardResponse(true, "Overclock применён в выбранной области")
            }

            UnitType.NULL_POINTER -> {
                inc(stats[request.playerId]?.spellsCast, request.unitType)
                castNullPointer(owner, targetX, targetY)
                return PlayCardResponse(true, "Null Pointer разыменован в выбранной точке")
            }

            else -> {
                val queue = spawnFactory ?: return PlayCardResponse(false, "Фабрика недоступна")
                if (queue.queue.size >= queueLimit(owner, queue.type)) {
                    runtime.memory += config.costMemory
                    runtime.cpu += config.costCpu
                    playerCardCooldowns[request.playerId]?.remove(request.unitType)
                    return PlayCardResponse(false, "Очередь фабрики заполнена")
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
                return PlayCardResponse(true, "${config.displayName} поставлен в очередь. Цель будет выбрана автоматически.")
            }
        }
    }

    fun buildFactory(request: BuildFactoryRequest): BuildFactoryResponse {
        if (!started.get()) return BuildFactoryResponse(false, "Игра ещё не началась")
        if (finished.get()) return BuildFactoryResponse(false, "Игра уже закончена")

        val runtime = playerRuntimes[request.playerId]
            ?: return BuildFactoryResponse(false, "Игрок не принадлежит этой комнате")

        val owner = OwnerType.fromPlayerIndex(runtime.playerIndex)
        val memoryCost = when (request.factoryType) {
            FactoryType.BASIC -> GameConfig.basicFactoryBuildMemoryCost
            FactoryType.SUPPORT -> GameConfig.supportFactoryBuildMemoryCost
        }
        val cpuCost = when (request.factoryType) {
            FactoryType.BASIC -> GameConfig.basicFactoryBuildCpuCost
            FactoryType.SUPPORT -> GameConfig.supportFactoryBuildCpuCost
        }

        if (runtime.memory < memoryCost) return BuildFactoryResponse(false, "Недостаточно выделенной памяти")
        if (runtime.cpu < cpuCost) return BuildFactoryResponse(false, "Недостаточно CPU")

        val core = findCore(owner) ?: return BuildFactoryResponse(false, "Ядро не найдено")
        val position = nextFactoryPosition(owner, request.factoryType)

        runtime.memory -= memoryCost
        runtime.cpu -= cpuCost
        runtime.factoriesBuilt += 1

        val factory = EntityFactory.createBuiltFactory(world, owner, request.factoryType, position.first, position.second)
        factoryQueues[factory.id] = FactoryQueueState(factory.id, owner, request.factoryType)
        recalculateFactoryMultipliers(owner, request.factoryType)
        stats[request.playerId]?.factoriesBuilt = stats[request.playerId]?.factoriesBuilt?.plus(1) ?: 1

        val t = core.get(Transform::class.java)
        if (t != null) pushText(owner, t.x, t.y + 90f, "Фабрика масштабирована: более параллельное производство")

        return BuildFactoryResponse(true, "Построена ${request.factoryType.name.lowercase().replaceFirstChar { it.uppercase() }} Factory")
    }

    fun forfeit(request: ForfeitMatchRequest): ForfeitMatchResponse {
        if (finished.get()) return ForfeitMatchResponse(false, "Матч уже завершён")
        val runtime = playerRuntimes[request.playerId]
            ?: return ForfeitMatchResponse(false, "Игрок не принадлежит этой комнате")

        finishGame(runtime.playerIndex, "Сдача: владелец экземпляра завершил матч")
        return ForfeitMatchResponse(true, "Сдача принята")
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
        unitCombat.updateUnitTargets(unitSpells)
        unitCombat.updateMovement(deltaSeconds, now)
        unitCombat.updateCombat(now, combatCallback, unitSpells)
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

                if (UnitConfig.isManualTargetCard(unit.typeName)) return@forEach

                when (unit.typeName) {
                    UnitType.CACHE_RUNNER -> unitTargeting.assignCacheRunnerTarget(entity, target, transform)
                    UnitType.GARBAGE_COLLECTOR -> unitTargeting.assignGarbageCollectorTarget(entity, target, transform)
                    UnitType.PATCH_HEALER -> unitTargeting.assignPatchHealerTarget(entity, target, transform)
                    UnitType.THREAD_GUARD,
                    UnitType.FIREWALL,
                    UnitType.MUTEX,
                    UnitType.SEMAPHORE,
                    UnitType.EXCEPTION_HANDLER -> unitTargeting.assignDefensivePosition(entity, target, transform)

                    UnitType.INJECTOR -> unitTargeting.assignStructureAttackTarget(entity, target, transform)
                    UnitType.COROUTINE_ARCHER,
                    UnitType.LOOP,
                    UnitType.RECURSIVE_CALL,
                    UnitType.STACK_FRAME -> unitTargeting.assignCombatTarget(entity, target, transform)

                    UnitType.POINTER,
                    UnitType.OBSERVER -> unitTargeting.assignObserverPosition(entity, target, transform)

                    UnitType.DEADLOCK,
                    UnitType.OVERCLOCK,
                    UnitType.NULL_POINTER -> {
                    }

                    UnitType.HEAP_BLOCK -> unitTargeting.assignDefensivePosition(entity, target, transform)
                    UnitType.ALLOCATOR,
                    UnitType.MEMORY_POOL,
                    UnitType.DMA_CONTROLLER,
                    UnitType.BUFFER -> unitTargeting.assignAllocatorTarget(entity, target, transform)

                    UnitType.CPU_SCHEDULER,
                    UnitType.LOAD_BALANCER,
                    UnitType.INTERRUPT_HANDLER -> unitTargeting.assignAllocatorTarget(entity, target, transform)
                }
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

    private fun chooseInitialAutoTargetEntity(entity: Entity, unitType: UnitType): Entity? {
        val transform = entity.get(Transform::class.java) ?: return null
        val config = UnitRegistry.getConfig(unitType)

        return when {
            config.memoryWorkPower > 0f && config.cpuRedirectPower <= 0f -> unitTargeting.bestMemoryNode(transform)
            config.cpuRedirectPower > 0f && config.memoryWorkPower <= 0f -> unitTargeting.bestCpuNodeForWorker(entity.owner(), transform)
            config.cpuRedirectPower > 0f && config.memoryWorkPower > 0f -> unitTargeting.bestMemoryNode(transform) ?: unitTargeting.bestCpuNodeForWorker(entity.owner(), transform)
            unitType == UnitType.GARBAGE_COLLECTOR -> unitTargeting.nearestDeadAlliedUnit(entity.owner(), transform)
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
        specialBehaviors.updateMemoryWorkers(
            deltaSeconds = deltaSeconds,
            now = now,
            playerRuntimes = playerRuntimes,
            textCallback = specialBehaviorsCallback,
            onMemoryAllocated = { playerId, batch, unitType ->
                stats[playerId]?.memoryAllocated = stats[playerId]?.memoryAllocated?.plus(batch) ?: batch
            },
            onProcessComplete = { entity ->
                val unit = entity.get(UnitComponent::class.java)
                completeAndRemoveProcess(entity, "${UnitRegistry.getConfig(unit?.typeName ?: UnitType.ALLOCATOR).displayName} completed")
            }
        )
    }

    private fun updateGarbageCollectorWork(now: Long) {
        specialBehaviors.updateGarbageCollectorWork(
            now = now,
            playerRuntimes = playerRuntimes,
            textCallback = specialBehaviorsCallback,
            onMemoryFreed = { playerId, freed ->
                stats[playerId]?.memoryFreed = stats[playerId]?.memoryFreed?.plus(freed) ?: freed
            },
            onProcessComplete = { entity ->
                completeAndRemoveProcess(entity, "Сборщик мусора закончил обход")
            }
        )
    }

    private fun updateSpecialProcesses(now: Long) {
        specialBehaviors.updateSpecialProcesses(
            now = now,
            textCallback = specialBehaviorsCallback,
            onProcessComplete = { entity ->
                completeAndRemoveProcess(entity, "Процесс завершён")
            }
        )
    }

    private fun updateAuras(now: Long) {
        specialBehaviors.updateAuras(now = now, textCallback = specialBehaviorsCallback)
    }

    private fun updatePatchHealer(now: Long) {
        specialBehaviors.updatePatchHealer(now = now, textCallback = specialBehaviorsCallback)
    }

    private fun updateDeaths(now: Long) {
        world.getEntities().forEach { entity ->
            val health = entity.get(Health::class.java) ?: return@forEach
            val unit = entity.get(UnitComponent::class.java) ?: return@forEach
            val process = entity.get(ProcessState::class.java) ?: return@forEach
            if (health.isDead && process.phase == ProcessPhase.RUNNING) {
                markDeadProcess(entity, now, "процесс упал; ожидание GC")
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
        finishGame(core.playerIndex, "Ядро уничтожено")
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
                    effects.stunnedUntil = now + UnitConfig.SpecialConstants.DEADLOCK_STUN_DURATION
                    pushText(enemy.owner(), transform.x, transform.y + 42f, "deadlocked: ожидание бесконечно")
                }
            }

        scope.launch {
            GameDispatcher.sendToAllPlayers(players, SystemMessageEvent("Deadlock блокировал выполнение врага"))
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
                    effects.overclockUntil = now + UnitConfig.SpecialConstants.OVERCLOCK_DURATION
                    pushText(owner, transform.x, transform.y + 42f, "overclocked: повышена пропускная способность")
                }
            }

        scope.launch {
            GameDispatcher.sendToAllPlayers(players, SystemMessageEvent("Overclock увеличил пропускную способность союзных процессов"))
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

        enemy.get(StatusEffects::class.java)?.stunnedUntil = now + UnitConfig.SpecialConstants.NULL_POINTER_STUN_DURATION
        enemy.get(Health::class.java)?.damage(UnitConfig.SpecialConstants.NULL_POINTER_DAMAGE)
        pushText(enemy.owner(), transform.x, transform.y + 42f, "NullPointerException")
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

    fun stop() {
        gameLoopJob?.cancel()
        scope.cancel()
    }
}
