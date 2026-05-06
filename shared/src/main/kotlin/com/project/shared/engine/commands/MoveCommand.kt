package com.project.shared.engine.commands

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("move")
data class MoveCommand(
    val playerId: Int,
    val entityId: Long,
    val targetX: Float,
    val targetY: Float
) : Command
