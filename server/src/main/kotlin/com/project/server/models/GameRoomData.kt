package com.project.server.models

data class GameRoomData(
    val id: String,
    val players: List<PlayerSession>
)
