package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.ListenReadyRequest
import com.project.shared.api.events.Event
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

class ListenSocket(private val playerId: Int) : WebSocket("/listen") {
    var onEvent: (Event) -> Unit = {}

    fun makeHandshake() {
        scope.launch {
            try {
                send<ListenReadyRequest>(ListenReadyRequest(playerId))
                listenIncoming()
            } catch (e: Exception) {
                Gdx.app.error("ListenSocket", "Handshake failed: ${e.message}", e)
            }
        }
    }

    private fun listenIncoming() {
        scope.launch {
            try {
                session?.incoming?.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val event = json.decodeFromString<Event>(frame.readText())
                        onEvent(event)
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("ListenSocket", "Listen failed: ${e.message}", e)
            }
        }
    }
}
