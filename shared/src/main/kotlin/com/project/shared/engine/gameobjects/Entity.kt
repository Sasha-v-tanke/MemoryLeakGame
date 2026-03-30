package com.project.shared.engine.gameobjects

import com.project.shared.engine.EntityState
import com.project.shared.engine.OwnerType
import com.project.shared.engine.gameobjects.components.Component
import com.project.shared.engine.gameobjects.components.Owner

data class Entity(
    var id: Long,
    val components: MutableMap<Class<out Component>, Component> = mutableMapOf()
) {
    fun <T : Component> add(component: T) {
        components[component.javaClass] = component
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Component> get(type: Class<T>): T? {
        return components[type] as? T
    }

    fun toState(): EntityState {
        val owner = get(Owner::class.java)

        return EntityState(
            id = id,
            owner = owner?.ownerType ?: OwnerType.WORLD,
            components = components.values.map { it }
        )
    }
}
