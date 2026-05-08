package com.project.server.engine

import com.project.shared.engine.config.EntityConfig
import com.project.shared.engine.config.GameConfig
import com.project.shared.engine.config.WorldObjectKind
import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.FactoryType
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.Owner
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.ResourceNodeType
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.Transform

object EntityFactory {
    fun createWorldObject(world: GameWorld, config: EntityConfig): Entity {
        val entity = world.createEntity()

        entity.add(
            Transform(
                x = config.x * GameConfig.worldWidth,
                y = config.y * GameConfig.worldHeight
            )
        )

        entity.add(Owner(config.owner))
        entity.add(Sprite(config.sprite, config.scale))

        when (config.kind) {
            WorldObjectKind.CORE -> {
                val playerIndex = config.owner.playerIndexOrNull()
                    ?: error("Ядро должно принадлежать игроку")

                entity.add(Core(playerIndex))
                entity.add(Health(config.health, config.health))
            }

            WorldObjectKind.BASIC_FACTORY -> {
                entity.add(Factory(FactoryType.BASIC))
                entity.add(Health(config.health, config.health))
            }

            WorldObjectKind.SUPPORT_FACTORY -> {
                entity.add(Factory(FactoryType.SUPPORT))
                entity.add(Health(config.health, config.health))
            }

            WorldObjectKind.CPU_NODE -> {
                entity.add(
                    ResourceNode(
                        nodeType = ResourceNodeType.CPU,
                        incomePerSecond = 1
                    )
                )
            }

            WorldObjectKind.MEMORY_NODE -> {
                entity.add(
                    ResourceNode(
                        nodeType = ResourceNodeType.MEMORY,
                        incomePerSecond = 0
                    )
                )
            }
        }

        return world.addEntity(entity)
    }

    fun createBuiltFactory(
        world: GameWorld,
        owner: OwnerType,
        factoryType: FactoryType,
        x: Float,
        y: Float
    ): Entity {
        val entity = world.createEntity()
        val sprite = when (factoryType) {
            FactoryType.BASIC -> "objects/factory_basic.png"
            FactoryType.SUPPORT -> "objects/factory_support.png"
        }

        entity.add(Transform(x, y))
        entity.add(Owner(owner))
        entity.add(Sprite(sprite, 0.95f))
        entity.add(Factory(factoryType, productionMultiplier = 1f, builtByPlayer = true))
        entity.add(Health(GameConfig.factoryBuildHealth, GameConfig.factoryBuildHealth))

        return world.addEntity(entity)
    }
}
