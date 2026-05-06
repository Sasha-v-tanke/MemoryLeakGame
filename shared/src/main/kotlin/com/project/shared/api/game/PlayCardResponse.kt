package com.project.shared.api.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("play_card_response")
data class PlayCardResponse(
    val success: Boolean,
    val description: String = ""
) : GameResponse
