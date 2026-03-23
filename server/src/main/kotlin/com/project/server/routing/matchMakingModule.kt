package com.project.server.routing

import com.project.server.service.MatchQueue
import com.project.server.models.PlayerSession
import com.project.server.service.SessionManager
import com.project.shared.api.matchmaking.CancelMatchResponse
import com.project.shared.api.matchmaking.ErrorResponse
import com.project.shared.api.matchmaking.FindMatchRequest
import com.project.shared.api.matchmaking.FindMatchResponse
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
    }
    routing {
        webSocket("/match/create") {
            try {
                incoming.consumeEach { frame ->
                    if (frame !is Frame.Text) return@consumeEach
                    try {
                        val req = json.decodeFromString<FindMatchRequest>(frame.readText())
                        val playerSession = SessionManager.getSession(req.playerId) ?: return@consumeEach

                        if (req.create) {
                            outgoing.send(Frame.Text(json.encodeToString(FindMatchResponse(true, "FindMatchResponse"))))
                        } else {
                            MatchQueue.removePlayer(playerSession)
                            outgoing.send(Frame.Text(json.encodeToString(CancelMatchResponse(true, "CancelMatchResponse"))))
                        }
                    } catch (e: Exception) {
                        outgoing.send(Frame.Text(json.encodeToString(ErrorResponse(false, ("error: " + e.message)))))
                    }
                }
            } catch (e: Exception) {
                //todo
            }
        }
    }
}