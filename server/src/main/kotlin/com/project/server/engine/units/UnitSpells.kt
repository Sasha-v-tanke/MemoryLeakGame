package com.project.server.engine.units

import com.project.server.engine.GameMath
import com.project.server.engine.GameWorld
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.StatusEffects
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit as UnitComponent
import com.project.shared.engine.entities.units.UnitType

/**
 * Логика для спеллов и специальных действий юнитов (DEADLOCK, OVERCLOCK, NULL_POINTER, OBSERVER, POINTER)
 */
class UnitSpells(private val world: GameWorld) {

    fun isValidTargetFor(
        attacker: UnitComponent,
        source: Entity,
        sourceTransform: Transform,
        combat: com.project.shared.engine.entities.components.CombatStats,
        candidate: Entity
    ): Boolean {
        val candidateTransform = candidate.get(Transform::class.java) ?: return false
        val distance = GameMath.distance(sourceTransform, candidateTransform)

        return when (attacker.typeName) {
            UnitType.THREAD_GUARD,
            UnitType.FIREWALL -> candidate.has(UnitComponent::class.java) && distance <= 260f

            UnitType.INJECTOR -> candidate.has(com.project.shared.engine.entities.components.Factory::class.java) ||
                              candidate.has(com.project.shared.engine.entities.components.Core::class.java) ||
                              candidate.has(UnitComponent::class.java)

            UnitType.COROUTINE_ARCHER -> candidate.has(UnitComponent::class.java) ||
                                        candidate.has(com.project.shared.engine.entities.components.Factory::class.java) ||
                                        candidate.has(com.project.shared.engine.entities.components.Core::class.java)

            UnitType.CACHE_RUNNER -> candidate.has(UnitComponent::class.java) && distance <= combat.attackRange * 1.65f

            UnitType.STACK_FRAME,
            UnitType.LOOP,
            UnitType.RECURSIVE_CALL -> candidate.has(UnitComponent::class.java)

            UnitType.HEAP_BLOCK -> candidate.has(UnitComponent::class.java) && distance <= 120f
            else -> false
        }
    }

    fun tryExceptionShield(target: Entity, now: Long): Boolean {
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
        return true
    }

    fun markNearestEnemy(source: Entity, unit: UnitComponent, transform: Transform, now: Long): Boolean {
        val range = if (unit.typeName == UnitType.OBSERVER)
            UnitConfig.SpecialConstants.OBSERVER_MARK_RANGE
        else
            UnitConfig.SpecialConstants.POINTER_MARK_RANGE

        val enemy = world.getAliveEntities()
            .filter { it.owner().isPlayer() && it.owner() != source.owner() && it.has(UnitComponent::class.java) }
            .minByOrNull {
                val t = it.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                GameMath.distance(transform, t)
            } ?: return false

        val enemyTransform = enemy.get(Transform::class.java) ?: return false
        if (GameMath.distance(transform, enemyTransform) > range) return false

        enemy.get(StatusEffects::class.java)?.markedUntil = now + UnitConfig.SpecialConstants.POINTER_MARK_DURATION
        return true
    }

    fun applyNullPointerDamage(target: Entity, now: Long): Boolean {
        val targetTransform = target.get(Transform::class.java) ?: return false
        target.get(StatusEffects::class.java)?.stunnedUntil = now + UnitConfig.SpecialConstants.NULL_POINTER_STUN_DURATION
        target.get(Health::class.java)?.damage(UnitConfig.SpecialConstants.NULL_POINTER_DAMAGE)
        return true
    }
}

