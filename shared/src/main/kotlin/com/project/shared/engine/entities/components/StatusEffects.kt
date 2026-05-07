package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("status_effects")
data class StatusEffects(
    var stunnedUntil: Long = 0L,
    var overclockUntil: Long = 0L,
    var markedUntil: Long = 0L,
    var protectedUntil: Long = 0L,
    var throughputUntil: Long = 0L,
    var exceptionShieldUsed: Boolean = false
) : Component {
    fun isStunned(now: Long): Boolean = stunnedUntil > now
    fun isOverclocked(now: Long): Boolean = overclockUntil > now
    fun isMarked(now: Long): Boolean = markedUntil > now
    fun isProtected(now: Long): Boolean = protectedUntil > now
    fun hasThroughputBoost(now: Long): Boolean = throughputUntil > now
}
