package com.project.shared.api.matchmaking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("cancel_match")
data class CancelMatchRequest(
    val playerId: Int
) : MatchMakingRequest
