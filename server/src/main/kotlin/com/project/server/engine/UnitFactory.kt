package com.project.server.engine

import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.AttackBehavior
import com.project.shared.engine.entities.components.CaptureBehavior
import com.project.shared.engine.entities.components.CombatStats
import com.project.shared.engine.entities.components.DefenseBehavior
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.Owner
import com.project.shared.engine.entities.components.ProcessState
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
        entity.add(Sprite(config.sprite, 1f))
        entity.add(
            Unit(
                typeName = config.unitType,
                role = config.role,
                costMemory = config.costMemory,
                costCpu = config.costCpu,
                allocatedMemory = config.allocatedMemory,
                memoryWorkPower = config.memoryWorkPower,
                cpuRedirectPower = config.cpuRedirectPower
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
            UnitRole.CAPTURE -> {
                entity.add(CaptureBehavior(x, y))
                entity.add(ProcessState())
            }

            UnitRole.SUPPORT -> entity.add(SupportBehavior(x, y))
            UnitRole.DEFENSE -> entity.add(DefenseBehavior(x, y))
            UnitRole.ATTACK -> entity.add(AttackBehavior(x, y))
            else -> {}

        }

        return world.addEntity(entity)
    }
}
