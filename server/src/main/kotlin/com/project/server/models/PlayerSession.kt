package com.project.server.models

import io.ktor.websocket.DefaultWebSocketSession

data class PlayerSession(
    val sessionId: String,
    val playerId: Int,
    val socket: DefaultWebSocketSession
)
