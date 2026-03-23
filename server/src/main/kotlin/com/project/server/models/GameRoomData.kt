package com.project.server.models

import kotlinx.serialization.Serializable

@Serializable
data class GameRoomData(
    val id: String,
    val players: List<PlayerSession>
)