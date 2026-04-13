package com.project.shared.engine.entities.components

import kotlinx.serialization.Serializable

@Serializable
sealed interface Behavior : Component {
    val targetX: Float?
    val targetY: Float?
}

@Serializable
data class WorkerBehavior(
    override val targetX: Float?,
    override val targetY: Float?,
    val isCarrying: Boolean = false
) : Behavior

@Serializable
data class CarrierBehavior(
    override val targetX: Float?,
    override val targetY: Float?,
    val currentLoad: Int = 0
) : Behavior

@Serializable
data class FighterBehavior(
    override val targetX: Float?,
    override val targetY: Float?,
    val targetEntityId: Long? = null
) : Behavior