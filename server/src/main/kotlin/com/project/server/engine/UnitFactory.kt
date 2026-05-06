package com.project.server.engine

import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.AttackBehavior
import com.project.shared.engine.entities.components.CaptureBehavior
import com.project.shared.engine.entities.components.CombatStats
import com.project.shared.engine.entities.components.DefenseBehavior
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.Owner
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.StatusEffects
import com.project.shared.engine.entities.components.SupportBehavior
import com.project.shared.engine.entities.components.Target
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit
import com.project.shared.engine.entities.components.Velocity
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitRole
import com.project.shared.engine.entities.units.UnitType

object UnitFactory {
    fun createUnit(
        world: GameWorld,
        unitType: UnitType,
        owner: OwnerType,
        x: Float,
        y: Float
    ): Entity {
        val config = UnitRegistry.getConfig(unitType)
        val entity = world.createEntity()

        entity.add(Transform(x, y))
        entity.add(Owner(owner))
        entity.add(Sprite(config.sprite, getVisualScale(unitType)))
        entity.add(
            Unit(
                type = config.unitType,
                role = config.role,
                costMemory = config.costMemory,
                costCpu = config.costCpu
            )
        )
        entity.add(Health(config.health, config.health))
        entity.add(
            CombatStats(
                damage = config.damage,
                attackRange = config.attackRange,
                attackCooldownMillis = config.attackCooldownMillis,
                moveSpeed = config.speed
            )
        )
        entity.add(Velocity(0f, 0f))
        entity.add(Target(targetX = x, targetY = y))
        entity.add(StatusEffects())

        when (config.role) {
            UnitRole.CAPTURE -> entity.add(CaptureBehavior(x, y))
            UnitRole.SUPPORT -> entity.add(SupportBehavior(x, y))
            UnitRole.DEFENSE -> entity.add(DefenseBehavior(x, y))
            UnitRole.ATTACK -> entity.add(AttackBehavior(x, y))
            UnitRole.SPELL -> {
                // Spells are handled before unit creation.
            }
        }

        return world.addEntity(entity)
    }

    private fun getVisualScale(unitType: UnitType): Float {
        return when (unitType) {
            UnitType.ALLOCATOR -> 1.0f
            UnitType.GARBAGE_COLLECTOR -> 1.08f
            UnitType.THREAD_GUARD -> 1.22f
            UnitType.INJECTOR -> 1.08f
            UnitType.DEADLOCK -> 1.0f
            UnitType.OVERCLOCK -> 1.0f
        }
    }
}
