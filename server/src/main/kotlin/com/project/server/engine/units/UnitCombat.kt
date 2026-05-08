package com.project.server.engine.units

import com.project.server.engine.GameMath
import com.project.server.engine.GameWorld
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.Behavior
import com.project.shared.engine.entities.components.CaptureBehavior
import com.project.shared.engine.entities.components.CombatStats
import com.project.shared.engine.entities.components.DefenseBehavior
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.StatusEffects
import com.project.shared.engine.entities.components.SupportBehavior
import com.project.shared.engine.entities.components.Target
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.AttackBehavior
import com.project.shared.engine.entities.components.Unit as UnitComponent
import com.project.shared.engine.entities.units.UnitType

/**
 * Система боевых действий и движения юнитов
 */
class UnitCombat(private val world: GameWorld) {

    interface CombatCallback {
        fun pushText(owner: OwnerType, x: Float, y: Float, text: String)
        fun onUnitKilled(attacker: Entity, victim: Entity, victimType: UnitType)
        fun markDeadProcess(entity: Entity, now: Long, message: String)
    }

    fun updateMovement(deltaSeconds: Float, now: Long) {
        world.getAliveEntities()
            .filter { it.has(UnitComponent::class.java) }
            .forEach { entity ->
                val transform = entity.get(Transform::class.java) ?: return@forEach
                val target = entity.get(Target::class.java) ?: return@forEach
                val combat = entity.get(CombatStats::class.java) ?: return@forEach
                val effects = entity.get(StatusEffects::class.java)

                if (effects?.isStunned(now) == true) return@forEach

                val speedMultiplier = when {
                    effects?.isOverclocked(now) == true -> UnitConfig.SpecialConstants.MOVEMENT_SPEED_OVERCLOCK_MULTIPLIER
                    effects?.hasThroughputBoost(now) == true -> UnitConfig.SpecialConstants.MOVEMENT_SPEED_THROUGHPUT_MULTIPLIER
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

    fun updateCombat(now: Long, callback: CombatCallback, spells: UnitSpells) {
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

                val cooldown = if (effects?.isOverclocked(now) == true)
                    (combat.attackCooldownMillis * UnitConfig.SpecialConstants.OVERCLOCK_COOLDOWN_REDUCTION).toLong()
                else
                    combat.attackCooldownMillis

                if (now - combat.lastAttackAt < cooldown) return@forEach

                combat.lastAttackAt = now

                var finalDamage = combat.damage
                val targetEffects = targetEntity.get(StatusEffects::class.java)
                if (targetEffects?.isMarked(now) == true)
                    finalDamage = (finalDamage * UnitConfig.SpecialConstants.MARKED_DAMAGE_MULTIPLIER).toInt().coerceAtLeast(finalDamage + 1)
                if (targetEffects?.isProtected(now) == true)
                    finalDamage = (finalDamage * UnitConfig.SpecialConstants.PROTECTED_DAMAGE_REDUCTION).toInt().coerceAtLeast(1)

                val fatal = targetHealth.current - finalDamage <= 0
                if (fatal && spells.tryExceptionShield(targetEntity, now)) {
                    targetHealth.current = 1
                    val tt = targetEntity.get(Transform::class.java)
                    if (tt != null) callback.pushText(targetEntity.owner(), tt.x, tt.y + 42f, "exception caught")
                    return@forEach
                }

                targetHealth.damage(finalDamage)

                if (unit.typeName == UnitType.STACK_FRAME) {
                    entity.get(com.project.shared.engine.entities.components.ProcessState::class.java)?.internalCounter = 1
                }

                val targetUnit = targetEntity.get(UnitComponent::class.java)
                if (targetHealth.isDead && targetUnit != null) {
                    callback.onUnitKilled(entity, targetEntity, targetUnit.typeName)
                    callback.markDeadProcess(targetEntity, now, "process crashed; memory still allocated")
                    callback.pushText(targetEntity.owner(), targetTransform.x, targetTransform.y + 42f, "dead object retained in memory")
                }
            }
    }

    fun updateUnitTargets(spells: UnitSpells) {
        val alive = world.getAliveEntities()
        val units = alive.filter { it.has(UnitComponent::class.java) }

        units.forEach { unit ->
            val unitComponent = unit.get(UnitComponent::class.java) ?: return@forEach
            if (unitComponent.typeName == UnitType.ALLOCATOR ||
                unitComponent.typeName == UnitType.GARBAGE_COLLECTOR ||
                unitComponent.typeName == UnitType.PATCH_HEALER)
                return@forEach

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
                            spells.isValidTargetFor(unitComponent, unit, unitTransform, combat, candidate)
                }
                .minByOrNull { candidate ->
                    val ct = candidate.get(Transform::class.java) ?: return@minByOrNull Float.MAX_VALUE
                    UnitTargeting(world).targetScore(unitComponent, candidate, GameMath.distance(unitTransform, ct))
                }

            if (enemy != null && (!currentTargetAlive || shouldReplaceTarget(unitComponent, unitTransform, combat, currentTarget, enemy))) {
                setEntityTarget(target, enemy)
            } else if (!currentTargetAlive) {
                restoreRallyTarget(unit, target, unitTransform)
            }
        }
    }

    private fun shouldReplaceTarget(
        attacker: UnitComponent,
        unitTransform: Transform,
        combat: CombatStats,
        currentTarget: Entity?,
        newTarget: Entity
    ): Boolean {
        if (currentTarget == null) return true
        val currentTransform = currentTarget.get(Transform::class.java) ?: return true
        val newTransform = newTarget.get(Transform::class.java) ?: return false
        val currentScore = UnitTargeting(world).targetScore(attacker, currentTarget, GameMath.distance(unitTransform, currentTransform))
        val newScore = UnitTargeting(world).targetScore(attacker, newTarget, GameMath.distance(unitTransform, newTransform))
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

    private fun setEntityTarget(target: Target, enemy: Entity) {
        val enemyTransform = enemy.get(Transform::class.java) ?: return
        target.targetEntityId = enemy.id
        target.targetX = enemyTransform.x
        target.targetY = enemyTransform.y
    }
}

