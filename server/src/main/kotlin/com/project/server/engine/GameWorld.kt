package com.project.server.engine

import com.project.shared.engine.entities.Entity

class GameWorld {
    private var entityIdCounter = 0L
    private val entities = mutableMapOf<Long, Entity>()

    fun createEntity(): Entity {
        return Entity(entityIdCounter++)
    }

    fun addEntity(entity: Entity) {
        entities[entity.id] = entity
    }

    fun removeEntity(id: Long) {
        entities.remove(id)
    }

    fun getEntities(): Collection<Entity> = entities.values
}