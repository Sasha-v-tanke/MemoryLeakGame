package com.project.server.routing

import com.project.server.service.MatchQueue
import com.project.server.service.SessionManager
import com.project.shared.api.JsonFormats
import com.project.shared.api.matchmaking.CancelMatchRequest
import com.project.shared.api.matchmaking.CancelMatchResponse
import com.project.shared.api.matchmaking.FindMatchRequest
import com.project.shared.api.matchmaking.FindMatchResponse
import com.project.shared.api.matchmaking.MatchMakingRequest
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.server.websocket.application
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString

fun Application.matchMakingModule() {
    val json = JsonFormats.default

    routing {
        webSocket("/matchmaking") {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach

                try {
                    when (val request = json.decodeFromString<MatchMakingRequest>(frame.readText())) {
                        is FindMatchRequest -> {
                            val playerSession = SessionManager.getSession(request.playerId)

                            if (playerSession == null) {
                                outgoing.send(
                                    Frame.Text(
                                        json.encodeToString(
                                            FindMatchResponse(false, "Listen session not found")
                                        )
                                    )
                                )
                            } else {
                                MatchQueue.addPlayer(playerSession)
                                outgoing.send(Frame.Text(json.encodeToString(FindMatchResponse(true))))
                            }
                        }

                        is CancelMatchRequest -> {
                            MatchQueue.removePlayer(request.playerId)
                            outgoing.send(Frame.Text(json.encodeToString(CancelMatchResponse(true))))
                        }
                    }
                } catch (e: Exception) {
                    application.log.error("Matchmaking error", e)
                    outgoing.send(
                        Frame.Text(
                            json.encodeToString(
                                FindMatchResponse(false, "Matchmaking error: ${e.message}")
                            )
                        )
                    )
                }
            }
        }
    }
}
