package com.project.server.engine.units

import com.project.server.engine.GameMath
import com.project.server.engine.GameWorld
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.ResourceNodeType
import com.project.shared.engine.entities.components.Target
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit as UnitComponent
import com.project.shared.engine.entities.units.UnitType

/**
 * Логика выбора целей для юнитов
 */
class UnitTargeting(private val world: GameWorld) {

    fun assignAllocatorTarget(entity: Entity, target: Target, transform: Transform) {
        val unit = entity.get(UnitComponent::class.java) ?: return

        val node = when {
            unit.memoryWorkPower > 0f && unit.cpuRedirectPower <= 0f -> bestMemoryNode(transform)
            unit.cpuRedirectPower > 0f && unit.memoryWorkPower <= 0f -> bestCpuNodeForWorker(entity.owner(), transform)
            unit.cpuRedirectPower > 0f && unit.memoryWorkPower > 0f -> {
                // Эвристика: если мало памяти, приоритет CPU узлам
                bestMemoryNode(transform) ?: bestCpuNodeForWorker(entity.owner(), transform)
            }
            else -> null
        } ?: return

        setEntityTarget(target, node)
    }

    fun assignCacheRunnerTarget(entity: Entity, target: Target, transform: Transform) {
        val node = bestCpuNodeForWorker(entity.owner(), transform)
        if (node != null) {
            setEntityTarget(target, node)
        } else {
            assignCombatTarget(entity, target, transform)
        }
    }

    fun assignGarbageCollectorTarget(entity: Entity, target: Target, transform: Transform) {
        val dead = nearestDeadAlliedUnit(entity.owner(), transform)
        if (dead != null) {
            setEntityTarget(target, dead)
        } else {
            assignSafeIdleNearCore(entity.owner(), target)
        }
    }

    fun assignPatchHealerTarget(entity: Entity, target: Target, transform: Transform) {
        val ally = world.getAliveEntities()
            .filter { it.owner() == entity.owner() && it.id != entity.id && it.has(UnitComponent::class.java) }
            .mapNotNull {
                val h = it.get(Health::class.java) ?: return@mapNotNull null
                val t = it.get(Transform::class.java) ?: return@mapNotNull null
                if (h.current < h.max) it to GameMath.distance(transform, t) else null
            }
            .minByOrNull { it.second }
            ?.first

        if (ally != null) {
            setEntityTarget(target, ally)
        } else {
            assignSafeIdleNearCore(entity.owner(), target)
        }
    }

    fun assignDefensivePosition(entity: Entity, target: Target, transform: Transform) {
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

    fun assignStructureAttackTarget(entity: Entity, target: Target, transform: Transform) {
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

        if (enemyStructure != null) {
            setEntityTarget(target, enemyStructure)
        } else {
            assignCombatTarget(entity, target, transform)
        }
    }

    fun assignCombatTarget(entity: Entity, target: Target, transform: Transform) {
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

        if (enemy != null) {
            setEntityTarget(target, enemy)
        } else {
            val enemyCore = findCore(entity.owner().opponent())
            if (enemyCore != null) setEntityTarget(target, enemyCore)
        }
    }

    fun assignObserverPosition(entity: Entity, target: Target, transform: Transform): Boolean {
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
                    return true
                }
            }
        }
        assignSafeIdleNearCore(entity.owner(), target)
        return false
    }

    fun assignSafeIdleNearCore(owner: OwnerType, target: Target) {
        val core = findCore(owner)?.get(Transform::class.java)
        if (core != null) {
            val forward = if (owner == OwnerType.PLAYER_1) 90f else -90f
            target.targetEntityId = null
            target.targetX = core.x + 70f
            target.targetY = core.y + forward
        }
    }

    fun bestMemoryNode(transform: Transform): Entity? {
        return world.entitiesWithComponent(ResourceNode::class.java)
            .filter { it.get(ResourceNode::class.java)?.nodeType == ResourceNodeType.MEMORY }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, t)
            }
    }

    fun bestCpuNodeForWorker(owner: OwnerType, transform: Transform): Entity? {
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

    fun defensivePointFor(owner: OwnerType, unitType: UnitType?): Pair<Float, Float> {
        val coreGameConfig = com.project.shared.engine.config.GameConfig
        val core = findCore(owner)?.get(Transform::class.java)
            ?: return coreGameConfig.worldWidth / 2f to coreGameConfig.worldHeight / 2f

        val forward = if (owner == OwnerType.PLAYER_1)
            UnitConfig.SpecialConstants.DEFENSIVE_FORWARD_OFFSET
        else
            -UnitConfig.SpecialConstants.DEFENSIVE_FORWARD_OFFSET

        val side = when (unitType) {
            UnitType.FIREWALL -> UnitConfig.SpecialConstants.DEFENSIVE_FIREWALL_SIDE_OFFSET
            UnitType.MUTEX -> UnitConfig.SpecialConstants.DEFENSIVE_MUTEX_SIDE_OFFSET
            UnitType.SEMAPHORE -> UnitConfig.SpecialConstants.DEFENSIVE_SEMAPHORE_SIDE_OFFSET
            UnitType.EXCEPTION_HANDLER -> UnitConfig.SpecialConstants.DEFENSIVE_EXCEPTION_HANDLER_OFFSET
            UnitType.HEAP_BLOCK -> UnitConfig.SpecialConstants.DEFENSIVE_HEAP_BLOCK_OFFSET
            else -> 0f
        }

        return (core.x + side).coerceIn(40f, coreGameConfig.worldWidth - 40f) to
                (core.y + forward).coerceIn(40f, coreGameConfig.worldHeight - 40f)
    }

    fun nearestEnemyUnit(enemyOwner: OwnerType, transform: Transform, radius: Float): Entity? {
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

    fun nearestDeadAlliedUnit(owner: OwnerType, transform: Transform): Entity? {
        return world.getDeadEntities()
            .filter { it.owner() == owner && it.has(UnitComponent::class.java) }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, t)
            }
    }

    fun findCore(owner: OwnerType): Entity? {
        return world.entitiesWithComponent(Core::class.java).firstOrNull { it.owner() == owner }
    }

    private fun setEntityTarget(target: Target, enemy: Entity) {
        val enemyTransform = enemy.get(Transform::class.java) ?: return
        target.targetEntityId = enemy.id
        target.targetX = enemyTransform.x
        target.targetY = enemyTransform.y
    }

    fun targetScore(attacker: UnitComponent, candidate: Entity, distance: Float): Float {
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
}

