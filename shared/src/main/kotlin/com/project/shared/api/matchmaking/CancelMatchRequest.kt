package com.project.shared.api.matchmaking

import kotlinx.serialization.Serializable

@Serializable
data class CancelMatchRequest(
    val playerId: Int
) : MatchMakingRequest