package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.game.GameStartEvent
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.api.game.PlayerReadyResponse
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GameSocket(endpoint: String) : WebSocket(endpoint) {
    fun sendPlayerReady(playerId: Int, roomId: String, onResult: (PlayerReadyResponse) -> Unit) {
        scope.launch {
            val result = try {
                connect()
                val s = session

                if (s == null) {
                    PlayerReadyResponse(false, "WebSocket is not connected").also {
                        Gdx.app.postRunnable { onResult(it) }
                    }
                    return@launch
                }

                s.send(Frame.Text(json.encodeToString(PlayerReadyRequest(playerId, roomId))))

                val responseText = withTimeoutOrNull(5_000) {
                    while (true) {
                        val frame = s.incoming.receive()
                        if (frame is Frame.Text) {
                            val text = frame.readText()
                            return@withTimeoutOrNull text
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    ""
                } ?: run {
                    Gdx.app.postRunnable { onResult(PlayerReadyResponse(false, "Server response timeout")) }
                    return@launch
                }

                json.decodeFromString<PlayerReadyResponse>(responseText)
            } catch (e: Exception) {
                Gdx.app.error("GameWebSocket", "Request failed: ${e.message}", e)
                PlayerReadyResponse(false, "Connection error: ${e.message ?: "unknown"}")
            }

            Gdx.app.postRunnable { onResult(result) }
        }
    }
}