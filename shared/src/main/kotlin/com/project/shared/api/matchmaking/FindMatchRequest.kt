package com.project.shared.api.matchmaking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("find_match")
data class FindMatchRequest(
    val playerId: Int
) : MatchMakingRequest
