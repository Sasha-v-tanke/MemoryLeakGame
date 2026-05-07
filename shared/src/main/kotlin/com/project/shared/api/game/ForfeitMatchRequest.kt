package com.project.shared.api.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("forfeit_match")
data class ForfeitMatchRequest(
    val playerId: Int,
    val roomId: String
) : GameRequest
