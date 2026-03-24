package com.project.shared.api.events

import kotlinx.serialization.Serializable

@Serializable
data class MatchFoundEvent(
    val roomId: String,
    val opponentId: Int,
    val index: Int,
    val description: String = ""
) : Event