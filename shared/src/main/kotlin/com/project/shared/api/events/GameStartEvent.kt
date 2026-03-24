package com.project.shared.api.events

import kotlinx.serialization.Serializable

@Serializable
data class GameStartEvent(
    val message: String = "GameStart"
) : Event