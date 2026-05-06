package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface Behavior : Component {
    val targetX: Float?
    val targetY: Float?
}

@Serializable
@SerialName("capture_behavior")
data class CaptureBehavior(
    override val targetX: Float?,
    override val targetY: Float?
) : Behavior

@Serializable
@SerialName("support_behavior")
data class SupportBehavior(
    override val targetX: Float?,
    override val targetY: Float?
) : Behavior

@Serializable
@SerialName("defense_behavior")
data class DefenseBehavior(
    override val targetX: Float?,
    override val targetY: Float?
) : Behavior

@Serializable
@SerialName("attack_behavior")
data class AttackBehavior(
    override val targetX: Float?,
    override val targetY: Float?,
    val targetEntityId: Long? = null
) : Behavior

@Serializable
@SerialName("spell_behavior")
data class SpellBehavior(
    override val targetX: Float?,
    override val targetY: Float?,
    val durationMillis: Long
) : Behavior
