package com.project.server.routing

import com.project.server.service.GameManager
import com.project.shared.api.game.GameRequest
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.api.game.PlayerReadyResponse
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Application.gameModule() {
    val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }
    routing {
        webSocket("/game") {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                try {
                    val request = json.decodeFromString<GameRequest>(frame.readText())
                    when (request) {
                        is PlayerReadyRequest -> {
                            GameManager.setPlayerReady(request)
                            outgoing.send(Frame.Text(json.encodeToString(PlayerReadyResponse(success = true))))
                        }
                    }
                } catch (e: Exception) {
                    outgoing.send(Frame.Text(json.encodeToString(PlayerReadyResponse(success = false, "Error: ${e.message}"))))
                    return@consumeEach
                }
            }
        }
    }
}