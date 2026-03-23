package com.project.shared.engine.commands

import kotlinx.serialization.Serializable

@Serializable
data class SpawnCommand(
    val playerId: Long,
    val entityType: Long,
    val targetX: Float,
    val targetY: Float
) : Command