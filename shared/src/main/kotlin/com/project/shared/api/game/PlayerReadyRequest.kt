package com.project.shared.api.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("player_ready")
data class PlayerReadyRequest(
    val playerId: Int,
    val roomId: String
) : GameRequest
