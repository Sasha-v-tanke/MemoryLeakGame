package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.matchmaking.CancelMatchResponse
import com.project.shared.api.matchmaking.FindMatchRequest
import com.project.shared.api.matchmaking.FindMatchResponse
import com.project.shared.api.matchmaking.MatchFoundEvent
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

class MatchMakingSocket(endpoint: String, private val playerId: Int) : WebSocket(endpoint) {

    var onFindMatchResponse: ((FindMatchResponse) -> Unit)? = null
    var onMatchFound: ((MatchFoundEvent) -> Unit)? = null
    var onCancelMatchResponse: ((CancelMatchResponse) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    fun findMatch() {
        scope.launch {
            connect()
            try {
                session?.send(Json.encodeToString(FindMatchRequest(playerId, true)))
            } catch (e: Exception) {
                Gdx.app.postRunnable { onError?.invoke(e) }
            }
        }
    }

    fun cancelMatch() {
        scope.launch {
            try {
                session?.send(Json.encodeToString(FindMatchRequest(playerId, false)))
            } catch (e: Exception) {
                Gdx.app.postRunnable { onError?.invoke(e) }
            }
        }
    }
}