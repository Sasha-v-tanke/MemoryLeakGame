package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.events.Event
import com.project.shared.api.ListenReadyRequest
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.events.MatchFoundEvent
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

class ListenSocket(private val playerId: Int) : WebSocket("/listen") {
    var onEvent: (Event) -> Unit = {}

    private fun listenIncoming() {
        scope.launch {
            try {
                session?.incoming?.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val event = Json.decodeFromString<Event>(frame.readText())
                        onEvent(event)
                    }
                }
            } catch (e: Exception) {
                Gdx.app.postRunnable {
                    println(e)
                }
            }
        }
    }

    fun makeHandshake() {
        scope.launch {
            try {
                send(ListenReadyRequest(playerId))
                listenIncoming()
            } catch (e: Exception) {
                Gdx.app.postRunnable { println(e) }
            }
        }
    }
}