package com.project.shared.api.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("match_found")
data class MatchFoundEvent(
    val roomId: String,
    val opponentId: Int,
    val index: Int,
    val description: String = "Opponent found"
) : Event
