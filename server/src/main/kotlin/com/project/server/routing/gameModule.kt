package com.project.server.routing

import com.project.server.service.GameManager
import com.project.shared.api.JsonFormats
import com.project.shared.api.game.BuildFactoryRequest
import com.project.shared.api.game.BuildFactoryResponse
import com.project.shared.api.game.ForfeitMatchRequest
import com.project.shared.api.game.ForfeitMatchResponse
import com.project.shared.api.game.GameRequest
import com.project.shared.api.game.PlayCardRequest
import com.project.shared.api.game.PlayCardResponse
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.api.game.PlayerReadyResponse
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.server.websocket.application
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString

fun Application.gameModule() {
    val json = JsonFormats.default

    routing {
        webSocket("/game") {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach

                try {
                    when (val request = json.decodeFromString<GameRequest>(frame.readText())) {
                        is PlayerReadyRequest -> {
                            GameManager.setPlayerReady(request)
                            outgoing.send(Frame.Text(json.encodeToString(PlayerReadyResponse(success = true))))
                        }

                        is PlayCardRequest -> {
                            outgoing.send(Frame.Text(json.encodeToString(GameManager.playCard(request))))
                        }

                        is BuildFactoryRequest -> {
                            outgoing.send(Frame.Text(json.encodeToString(GameManager.buildFactory(request))))
                        }

                        is ForfeitMatchRequest -> {
                            outgoing.send(Frame.Text(json.encodeToString(GameManager.forfeit(request))))
                        }
                    }
                } catch (e: Exception) {
                    application.log.error("Game request error", e)

                    outgoing.send(
                        Frame.Text(
                            json.encodeToString(
                                PlayCardResponse(
                                    success = false,
                                    description = "Game error: ${e.message}"
                                )
                            )
                        )
                    )
                }
            }
        }
    }
}
