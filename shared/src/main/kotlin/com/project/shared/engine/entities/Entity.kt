package com.project.shared.engine.entities

import com.project.shared.engine.EntityState
import com.project.shared.engine.entities.components.Component
import com.project.shared.engine.entities.components.Owner

data class Entity(
    var id: Long,
    val components: MutableMap<Class<out Component>, Component> = mutableMapOf()
) {
    fun <T : Component> add(component: T): Entity {
        components[component.javaClass] = component
        return this
    }

    fun remove(type: Class<out Component>): Entity {
        components.remove(type)
        return this
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Component> get(type: Class<T>): T? {
        return components[type] as? T
    }

    fun has(type: Class<out Component>): Boolean {
        return components.containsKey(type)
    }

    fun owner(): OwnerType {
        return get(Owner::class.java)?.ownerType ?: OwnerType.WORLD
    }

    fun toState(): EntityState {
        return EntityState(
            id = id,
            owner = owner(),
            components = components.values.toList()
        )
    }
}
