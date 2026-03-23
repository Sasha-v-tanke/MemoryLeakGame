package com.project.shared.api.game

import com.project.shared.api.Request
import kotlinx.serialization.Serializable

@Serializable
data class PlayerReadyRequest(
    val playerId: Int,
    val roomId: String
) : Request
