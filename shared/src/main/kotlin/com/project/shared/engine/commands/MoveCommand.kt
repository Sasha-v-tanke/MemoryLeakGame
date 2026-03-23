package com.project.shared.engine.commands

import kotlinx.serialization.Serializable

@Serializable
data class MoveCommand(
    val playerId: Long,
    val entityId: Long,
    val targetX: Float,
    val targetY: Float
) : Command

