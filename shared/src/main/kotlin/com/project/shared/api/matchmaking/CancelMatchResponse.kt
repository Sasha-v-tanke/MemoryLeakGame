package com.project.shared.api.matchmaking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("cancel_match_response")
data class CancelMatchResponse(
    val success: Boolean,
    val description: String = ""
) : MatchMakingResponse
