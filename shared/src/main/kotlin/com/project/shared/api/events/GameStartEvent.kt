package com.project.shared.api.events

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("game_start")
data class GameStartEvent(
    val message: String = "Game started"
) : Event
