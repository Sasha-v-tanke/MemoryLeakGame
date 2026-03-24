package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.game.GameRequest
import com.project.shared.api.game.GameResponse
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.api.game.PlayerReadyResponse
import com.project.shared.api.matchmaking.FindMatchResponse
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameSocket : WebSocket("game") {
    fun sendPlayerReady(playerId: Int, roomId: String, onResult: (GameResponse) -> Unit) {
        scope.launch {
            try {
                send<GameRequest>(PlayerReadyRequest(playerId, roomId))
                val response = receiveMessage<PlayerReadyResponse>()
                    ?: PlayerReadyResponse(false, "Server response timeout")
                onResult(response)
            } catch (e: Exception) {
                Gdx.app.error("GameWebSocket", "Request failed: ${e.message}", e)
                onResult(PlayerReadyResponse(false, "Connection error: ${e.message}"))
            }
        }
    }
}