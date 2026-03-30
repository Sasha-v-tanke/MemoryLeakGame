package com.project.server.routing

import com.project.server.service.MatchQueue
import com.project.server.service.SessionManager
import com.project.shared.api.matchmaking.CancelMatchRequest
import com.project.shared.api.matchmaking.CancelMatchResponse
import com.project.shared.api.matchmaking.FindMatchRequest
import com.project.shared.api.matchmaking.FindMatchResponse
import com.project.shared.api.matchmaking.MatchMakingRequest
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

fun Application.matchMakingModule() {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }

    routing {
        webSocket("/matchmaking") {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach

                try {
                    val request = json.decodeFromString<MatchMakingRequest>(frame.readText())

                    when (request) {
                        is FindMatchRequest -> {
                            val playerSession = SessionManager.getSession(request.playerId)
                            if (playerSession != null) {
                                MatchQueue.addPlayer(playerSession)
                                outgoing.send(Frame.Text(json.encodeToString(FindMatchResponse(true))))
                            } else {
                                outgoing.send(
                                    Frame.Text(
                                        json.encodeToString(
                                            FindMatchResponse(false, "Session not found")
                                        )
                                    )
                                )
                            }
                        }

                        is CancelMatchRequest -> {
                            val playerSession = SessionManager.getSession(request.playerId)
                            if (playerSession != null) {
                                MatchQueue.removePlayer(playerSession)
                                outgoing.send(Frame.Text(json.encodeToString(CancelMatchResponse(true))))
                            } else {
                                outgoing.send(
                                    Frame.Text(
                                        json.encodeToString(
                                            CancelMatchResponse(false, "Session not found")
                                        )
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Matchmaking error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}