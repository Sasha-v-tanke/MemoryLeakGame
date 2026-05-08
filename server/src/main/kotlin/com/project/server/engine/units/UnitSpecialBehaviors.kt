package com.project.server.engine.units

import com.project.server.engine.GameMath
import com.project.server.engine.GameWorld
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.ProcessPhase
import com.project.shared.engine.entities.components.ProcessState
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.ResourceNodeType
import com.project.shared.engine.entities.components.StatusEffects
import com.project.shared.engine.entities.components.Target
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit as UnitComponent
import com.project.shared.engine.entities.units.UnitType

class UnitSpecialBehaviors(private val world: GameWorld) {

    interface TextEventCallback {
        fun pushText(owner: OwnerType, x: Float, y: Float, text: String)
    }

    fun updateMemoryWorkers(
        deltaSeconds: Float,
        now: Long,
        playerRuntimes: Map<Int, com.project.server.models.PlayerRuntime>,
        textCallback: TextEventCallback,
        onMemoryAllocated: (playerId: Int, batch: Int, unitType: UnitType) -> Unit,
        onProcessComplete: (entity: Entity) -> Unit
    ) {
        val targeting = UnitTargeting(world)

        world.getAliveEntities()
            .filter {
                val unit = it.get(com.project.shared.engine.entities.components.Unit::class.java) ?: return@filter false
                unit.memoryWorkPower > 0f
            }
            .forEach { worker ->
                println("[step 1] Worker: $worker")
                val unit = worker.get(com.project.shared.engine.entities.components.Unit::class.java) ?: return@forEach

                println("[step 1.5] Worker: $worker")
                val transform = worker.get(Transform::class.java) ?: return@forEach
                val process = worker.get(ProcessState::class.java) ?: return@forEach
                val targetNode = worker.get(Target::class.java)?.targetEntityId?.let { world.getEntity(it) }
                    ?: targeting.bestMemoryNode(transform)
                    ?: return@forEach

                println("[step 2] Worker: $worker")
                val node = targetNode.get(ResourceNode::class.java) ?: return@forEach
                if (node.nodeType != ResourceNodeType.MEMORY) return@forEach

                println("[step 3] Worker: $worker")
                val nodeTransform = targetNode.get(Transform::class.java) ?: return@forEach
                if (GameMath.distance(transform, nodeTransform) > node.captureRadius) return@forEach

                println("[step 4] Worker: $worker")
                if (process.phaseStartedAt <= 0L || process.lastEvent != "MEMORY_WORK") {
                    process.phaseStartedAt = now
                    process.internalCounter = 0
                    process.lastEvent = "MEMORY_WORK"
                    process.phase = ProcessPhase.WORKING
                    textCallback.pushText(worker.owner(), transform.x, transform.y + 42f, UnitConfig.memoryWorkStartText(unit.typeName))
                }

                process.internalCounter += (unit.memoryWorkPower * deltaSeconds).toInt().coerceAtLeast(1)

                val workDuration = UnitConfig.SpecialConstants.MEMORY_WORK_DURATION
                println("Time waiting: ${now - process.phaseStartedAt} / $workDuration, progress: ${process.internalCounter}")
                if (now - process.phaseStartedAt < workDuration) return@forEach

                val playerIndex = worker.owner().playerIndexOrNull() ?: return@forEach
                val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == playerIndex } ?: return@forEach
                val batch = UnitConfig.memoryBatchFor(unit.typeName)

                runtime.memory += batch
                runtime.memoryAllocatedTotal += batch
                onMemoryAllocated(runtime.playerId, batch, unit.typeName)

                textCallback.pushText(worker.owner(), transform.x, transform.y + 42f, "+$batch Memory allocated")
                onProcessComplete(worker)
            }
    }

    fun updateGarbageCollectorWork(
        now: Long,
        playerRuntimes: Map<Int, com.project.server.models.PlayerRuntime>,
        textCallback: TextEventCallback,
        onMemoryFreed: (playerId: Int, freed: Int) -> Unit,
        onProcessComplete: (entity: Entity) -> Unit
    ) {
        val targeting = UnitTargeting(world)

        world.getAliveEntities()
            .filter { it.get(UnitComponent::class.java)?.typeName == UnitType.GARBAGE_COLLECTOR }
            .forEach { collector ->
                val transform = collector.get(Transform::class.java) ?: return@forEach
                val process = collector.get(ProcessState::class.java) ?: return@forEach
                val dead = targeting.nearestDeadAlliedUnit(collector.owner(), transform)

                if (dead == null) {
                    process.lastEvent = "waiting for garbage"
                    return@forEach
                }

                val deadTransform = dead.get(Transform::class.java) ?: return@forEach
                val distance = GameMath.distance(transform, deadTransform)
                if (distance > UnitConfig.SpecialConstants.GC_TARGET_SEARCH_RANGE) {
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
                    textCallback.pushText(collector.owner(), transform.x, transform.y + 42f, "marking unreachable object...")
                }

                if (now - process.phaseStartedAt < UnitConfig.SpecialConstants.GARBAGE_COLLECTOR_WORK_DURATION) return@forEach

                val freed = dead.get(UnitComponent::class.java)?.allocatedMemory ?: 0

                val playerIndex = collector.owner().playerIndexOrNull()
                if (playerIndex != null) {
                    val runtime = playerRuntimes.values.firstOrNull { it.playerIndex == playerIndex }
                    if (runtime != null) {
                        runtime.memory += freed
                        runtime.memoryFreedTotal += freed
                        onMemoryFreed(runtime.playerId, freed)
                    }
                }

                world.removeEntity(dead.id)
                textCallback.pushText(collector.owner(), transform.x, transform.y + 42f, "swept garbage: +$freed Memory")
                onProcessComplete(collector)
            }
    }

    fun updateSpecialProcesses(now: Long, textCallback: TextEventCallback, onProcessComplete: (entity: Entity) -> Unit) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
            .forEach { entity ->
                val unit = entity.get(UnitComponent::class.java) ?: return@forEach
                val process = entity.get(ProcessState::class.java) ?: return@forEach
                val transform = entity.get(Transform::class.java) ?: return@forEach

                when (unit.typeName) {
                    UnitType.STACK_FRAME -> {
                        if (process.internalCounter <= 0 && now - process.phaseStartedAt > UnitConfig.SpecialConstants.STACK_FRAME_LIFETIME) {
                            onProcessComplete(entity)
                        }
                    }

                    UnitType.RECURSIVE_CALL -> {
                        if (process.nextActionAt <= 0L) process.nextActionAt = now + UnitConfig.SpecialConstants.RECURSIVE_CALL_CHECK_INTERVAL
                        if (now >= process.nextActionAt) {
                            process.internalCounter += 1
                            process.nextActionAt = now + UnitConfig.SpecialConstants.RECURSIVE_CALL_CHECK_INTERVAL
                            textCallback.pushText(entity.owner(), transform.x, transform.y + 42f, "recursive depth ${process.internalCounter}")
                            if (process.internalCounter >= UnitConfig.SpecialConstants.RECURSIVE_CALL_STACK_OVERFLOW_LIMIT) {
                                entity.get(Health::class.java)?.damage(UnitConfig.SpecialConstants.RECURSIVE_CALL_OVERFLOW_DAMAGE)
                                textCallback.pushText(entity.owner(), transform.x, transform.y + 42f, "stack overflow risk")
                            }
                        }
                    }

                    UnitType.LOOP -> {
                        if (process.nextActionAt <= 0L) process.nextActionAt = now + UnitConfig.SpecialConstants.LOOP_TICK_INTERVAL
                        if (now >= process.nextActionAt) {
                            process.nextActionAt = now + UnitConfig.SpecialConstants.LOOP_TICK_INTERVAL
                            textCallback.pushText(entity.owner(), transform.x, transform.y + 42f, "loop iteration")
                        }
                    }

                    else -> {}
                }
            }
    }

    fun updateAuras(now: Long, textCallback: TextEventCallback) {
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
                                textCallback.pushText(entity.owner(), allyTransform.x, allyTransform.y + 42f, "mutex protected critical section")
                            }
                        }
                    }

                    UnitType.SEMAPHORE -> {
                        applyAllyAura(entity.owner(), transform, 135f) { ally, allyTransform ->
                            ally.get(StatusEffects::class.java)?.throughputUntil = now + 350L
                            if ((now / 1000L) % 3L == 0L) {
                                textCallback.pushText(entity.owner(), allyTransform.x, allyTransform.y + 42f, "semaphore permits throughput")
                            }
                        }
                    }

                    UnitType.EXCEPTION_HANDLER -> {
                        applyAllyAura(entity.owner(), transform, 120f) { ally, allyTransform ->
                            val effects = ally.get(StatusEffects::class.java) ?: return@applyAllyAura
                            if (!effects.exceptionShieldUsed && (now / 1000L) % 4L == 0L) {
                                textCallback.pushText(entity.owner(), allyTransform.x, allyTransform.y + 42f, "exception handler ready")
                            }
                        }
                    }

                    else -> {}
                }
            }
    }

    fun updatePatchHealer(
        now: Long,
        textCallback: TextEventCallback
    ) {
        world.getAliveEntities()
            .filter { it.get(UnitComponent::class.java)?.typeName == UnitType.PATCH_HEALER }
            .forEach { healer ->
                val transform = healer.get(Transform::class.java) ?: return@forEach
                val combat = healer.get(com.project.shared.engine.entities.components.CombatStats::class.java) ?: return@forEach
                if (now - combat.lastAttackAt < UnitConfig.SpecialConstants.PATCH_HEALER_COOLDOWN) return@forEach

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
                    if (allyTransform != null && GameMath.distance(transform, allyTransform) > UnitConfig.SpecialConstants.PATCH_HEALER_SEARCH_RANGE) {
                        healer.get(Target::class.java)?.apply {
                            targetEntityId = ally.id
                            targetX = allyTransform.x
                            targetY = allyTransform.y
                        }
                        return@forEach
                    }

                    val health = ally.get(Health::class.java) ?: return@forEach
                    combat.lastAttackAt = now
                    health.heal(UnitConfig.SpecialConstants.PATCH_HEALER_HEALING)
                    textCallback.pushText(healer.owner(), transform.x, transform.y + 42f, "patched live process")
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

    private fun find(targeting: UnitTargeting): UnitTargeting = targeting
}

