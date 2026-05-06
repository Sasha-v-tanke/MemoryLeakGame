package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("combat_stats")
data class CombatStats(
    val damage: Int,
    val attackRange: Float,
    val attackCooldownMillis: Long,
    var lastAttackAt: Long = 0L,
    val moveSpeed: Float
) : Component
