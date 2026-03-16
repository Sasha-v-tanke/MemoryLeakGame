package com.project.server.routing

import com.project.server.models.MatchQueue
import com.project.server.models.PlayerSession
import com.project.server.models.SessionManager
import com.project.shared.api.CancelMatchResponse
import com.project.shared.api.ErrorResponse
import com.project.shared.api.FindMatchRequest
import com.project.shared.api.FindMatchResponse
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
        webSocket("/create_match") {
            val sessionId = UUID.randomUUID().toString()
            lateinit var playerSession: PlayerSession
            try {
                incoming.consumeEach { frame ->
                    if (frame !is Frame.Text) return@consumeEach
                    try {
                        val req = json.decodeFromString<FindMatchRequest>(frame.readText())
                        if (req.create) {
                            playerSession = PlayerSession(sessionId, req.playerId, this)
                            SessionManager.addSession(playerSession)
                            MatchQueue.addPlayer(playerSession)
                            playerSession.initialized = true
                            outgoing.send(Frame.Text(json.encodeToString(FindMatchResponse(true, "FindMatchResponse"))))
                        } else {
                            playerSession = SessionManager.getSession(req.playerId)
                                ?: PlayerSession(sessionId, req.playerId, this)
                            MatchQueue.removePlayer(playerSession)
                            SessionManager.removeSession(playerSession.sessionId)
                            outgoing.send(Frame.Text(json.encodeToString(CancelMatchResponse(true, "CancelMatchResponse"))))
                        }
                    } catch (e: Exception) {
                        outgoing.send(Frame.Text(json.encodeToString(ErrorResponse(false, ("error: " + e.message)))))
                    }
                }
            } finally {
                playerSession.takeIf { it.initialized }?.let {
                    MatchQueue.removePlayer(it)
                    SessionManager.removeSession(it.sessionId)
                }
            }
        }
    }
}