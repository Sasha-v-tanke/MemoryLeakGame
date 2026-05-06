package com.project.shared.api.matchmaking

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("find_match_response")
data class FindMatchResponse(
    val success: Boolean,
    val description: String = ""
) : MatchMakingResponse
