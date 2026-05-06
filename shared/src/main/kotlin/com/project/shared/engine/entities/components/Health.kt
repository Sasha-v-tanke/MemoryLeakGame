package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("health")
data class Health(
    var current: Int,
    val max: Int
) : Component {
    val isDead: Boolean
        get() = current <= 0

    fun damage(amount: Int) {
        current = (current - amount).coerceAtLeast(0)
    }

    fun heal(amount: Int) {
        current = (current + amount).coerceAtMost(max)
    }
}
