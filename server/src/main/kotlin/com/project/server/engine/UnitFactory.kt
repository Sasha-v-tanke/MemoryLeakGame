package com.project.server.engine

import com.project.shared.engine.config.GameConfig
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitType
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.Owner
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.Target
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit

object UnitFactory {
    fun create(
        world: GameWorld,
        unitType: UnitType,
        owner: OwnerType,
        x: Float,
        y: Float
    ): Entity {
        val config = UnitRegistry.getConfig(unitType)
        val entity = world.createEntity()

        entity.add(
            Transform(
                x = x * GameConfig.worldWidth,
                y = y * GameConfig.worldHeight
            )
        )
        entity.add(Owner(owner))
        entity.add(Sprite(config.sprite, 1f))
        entity.add(
            Unit(
                type = config.unitType,
                role = config.role,
                costMemory = config.costRAM, // пока временно, потом переименуем/разнесем
                costCpu = config.costCPU,
                maxHealth = config.health.toInt(),
                damage = 0,
                speed = config.speed,
                range = 0f
            )
        )
        entity.add(Health(config.health.toInt(), config.health.toInt()))
        entity.add(Target(null))

        world.addEntity(entity)
        return entity
    }
}