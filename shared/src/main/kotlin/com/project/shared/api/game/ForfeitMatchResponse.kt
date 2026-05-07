package com.project.shared.api.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("forfeit_match_response")
data class ForfeitMatchResponse(
    val success: Boolean,
    val description: String = ""
) : GameResponse
