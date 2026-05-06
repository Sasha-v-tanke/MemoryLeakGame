package com.project.shared.api.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("game_over")
data class GameOverEvent(
    val winnerPlayerId: Int,
    val loserPlayerId: Int,
    val reason: String = "Core destroyed"
) : Event
