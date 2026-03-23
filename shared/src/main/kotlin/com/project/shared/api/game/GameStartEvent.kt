package com.project.shared.api.game

import com.project.shared.api.Event
import kotlinx.serialization.Serializable

@Serializable
data class GameStartEvent(
    val message: String = "GameStart"
) : Event
