package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.Event
import com.project.shared.api.auth.ListenReadyRequest
import com.project.shared.api.game.GameStartEvent
import com.project.shared.api.matchmaking.MatchFoundEvent
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

    override fun connect() {
        super.connect()

        makeHandshake()
    }

    private val module = SerializersModule {
        polymorphic(Event::class) {
            subclass(GameStartEvent::class)
            subclass(MatchFoundEvent::class)
        }
    }

    private fun listenIncoming() {
        scope.launch {
            try {
                session?.incoming?.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        dispatchMessage(text)
                    }
                }
            } catch (e: Exception) {
                Gdx.app.postRunnable {
                    //todo
                }
            }
        }
    }

    private fun makeHandshake() {
        scope.launch {
            send(ListenReadyRequest(playerId))
            listenIncoming()
        }
    }

    private fun dispatchMessage(text: String) {
        try {
            val event = Json.decodeFromString<Event>(text)
            onEvent(event)
        } catch (e: Exception) {
            Gdx.app.postRunnable {
                //todo
            }
        }
    }
}