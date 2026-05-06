package com.project.shared.engine.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("spawn")
data class SpawnCommand(
    val playerId: Int,
    val entityType: String,
    val targetX: Float,
    val targetY: Float
) : Command
