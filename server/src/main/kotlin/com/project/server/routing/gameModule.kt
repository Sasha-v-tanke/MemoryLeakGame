package com.project.server.routing

import com.project.server.service.GameManager
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
    val json = Json { ignoreUnknownKeys = true }
    routing {
        webSocket("/game/player/ready") {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach

                var request: PlayerReadyRequest? = null
                try {
                    request = json.decodeFromString<PlayerReadyRequest>(frame.readText())
                    GameManager.setPlayerReady(request)
                } catch (e: Exception) {
                    outgoing.send(Frame.Text(json.encodeToString(PlayerReadyResponse(success = false))))
                    return@consumeEach
                }

                outgoing.send(Frame.Text(json.encodeToString(PlayerReadyResponse(success = true))))

            }
        }

    }
}