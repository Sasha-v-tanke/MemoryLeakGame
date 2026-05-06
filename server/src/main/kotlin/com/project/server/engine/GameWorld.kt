package com.project.server.engine

import com.project.shared.engine.entities.Entity
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.Component
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.Owner
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class GameWorld {
    private val entityIdCounter = AtomicLong(1L)
    private val entities = ConcurrentHashMap<Long, Entity>()

    fun createEntity(): Entity {
        return Entity(entityIdCounter.getAndIncrement())
    }

    fun addEntity(entity: Entity): Entity {
        entities[entity.id] = entity
        return entity
    }

    fun removeEntity(id: Long) {
        entities.remove(id)
    }

    fun getEntity(id: Long): Entity? {
        return entities[id]
    }

    fun getEntities(): Collection<Entity> {
        return entities.values
    }

    fun getAliveEntities(): List<Entity> {
        return entities.values.filter {
            val health = it.get(Health::class.java)
            health == null || !health.isDead
        }
    }

    fun removeDeadNonCoreEntities() {
        val toRemove = entities.values
            .filter {
                val health = it.get(Health::class.java)
                health != null && health.isDead && !it.has(com.project.shared.engine.entities.components.Core::class.java)
            }
            .map { it.id }

        toRemove.forEach { id ->
            entities.remove(id)
        }
    }

    fun entitiesWithOwner(owner: OwnerType): List<Entity> {
        return entities.values.filter {
            it.get(Owner::class.java)?.ownerType == owner
        }
    }

    fun entitiesWithComponent(type: Class<out Component>): List<Entity> {
        return entities.values.filter { it.has(type) }
    }
}
