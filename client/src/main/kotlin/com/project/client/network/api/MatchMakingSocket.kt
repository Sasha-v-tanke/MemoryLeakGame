package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.matchmaking.*
import kotlinx.coroutines.*

class MatchMakingSocket(private val playerId: Int) : WebSocket("matchmaking") {
    var onFindError: ((Exception) -> Unit)? = null
    var onCancelError: ((Exception) -> Unit)? = null

    fun findMatch(callback: (FindMatchResponse) -> Unit) {
        scope.launch {
            try {
                send<MatchMakingRequest>(FindMatchRequest(playerId))
                val response = receiveMessage<FindMatchResponse>()
                    ?: FindMatchResponse(false, "No response is found")
                callback(response)
            } catch (e: Exception) {
                Gdx.app.postRunnable { onFindError?.invoke(e) }
                callback(FindMatchResponse(false, "Error: ${e.message}"))
            }
        }
    }

    fun cancelMatch(callback: (CancelMatchResponse) -> Unit) {
        scope.launch {
            try {
                send<MatchMakingRequest>(CancelMatchRequest(playerId))
                val response = receiveMessage<CancelMatchResponse>()
                if (response != null) {
                    callback(response)
                } else {
                    callback(CancelMatchResponse(false, "No response is found"))
                }
            } catch (e: Exception) {
                Gdx.app.postRunnable { onCancelError?.invoke(e) }
                callback(CancelMatchResponse(false, "Error: ${e.message}"))
            }
        }
    }
}