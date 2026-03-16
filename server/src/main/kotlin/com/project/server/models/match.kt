package com.project.server.models

import io.ktor.websocket.DefaultWebSocketSession
import kotlinx.serialization.Serializable

@Serializable
data class PlayerSession(
    val sessionId: String,
    val playerId: Int,
    val socket: DefaultWebSocketSession,
    var initialized: Boolean = false
)


@Serializable
data class GameRoom(
    val id: String,
    val players: List<PlayerSession>
)

