package com.project.shared.engine.gameobjects.components

import com.project.shared.engine.OwnerType
import kotlinx.serialization.Serializable

interface Component


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
}

@Serializable
data class Transform(var x: Float, var y: Float) : Component

@Serializable
data class Velocity(var dx: Float, var dy: Float) : Component

@Serializable
data class Owner(val type: OwnerType) : Component

@Serializable
data class Sprite(val textureId: String) : Component