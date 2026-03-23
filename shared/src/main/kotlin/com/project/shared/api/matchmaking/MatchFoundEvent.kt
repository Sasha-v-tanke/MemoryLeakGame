package com.project.shared.api.matchmaking

import com.project.shared.api.Event
import kotlinx.serialization.Serializable


@Serializable
data class MatchFoundEvent(
    val roomId: String,
    val opponentId: Int,
    val index: Int,
    val type: String = "Error"
) : Event