package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("status_effects")
data class StatusEffects(
    var stunnedUntil: Long = 0L,
    var overclockUntil: Long = 0L
) : Component {
    fun isStunned(now: Long): Boolean = stunnedUntil > now
    fun isOverclocked(now: Long): Boolean = overclockUntil > now
}
