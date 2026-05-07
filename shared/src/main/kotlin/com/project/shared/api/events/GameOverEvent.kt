package com.project.shared.api.events

import com.project.shared.engine.MatchStats
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("game_over")
data class GameOverEvent(
    val winnerPlayerId: Int,
    val loserPlayerId: Int,
    val reason: String = "Core destroyed",
    val stats: MatchStats = MatchStats()
) : Event
