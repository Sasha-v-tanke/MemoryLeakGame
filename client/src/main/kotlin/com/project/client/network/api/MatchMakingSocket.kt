package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.matchmaking.CancelMatchRequest
import com.project.shared.api.matchmaking.CancelMatchResponse
import com.project.shared.api.matchmaking.FindMatchRequest
import com.project.shared.api.matchmaking.FindMatchResponse
import com.project.shared.api.matchmaking.MatchMakingRequest
import kotlinx.coroutines.launch

class MatchMakingSocket(private val playerId: Int) : WebSocket("matchmaking") {
    var onFindError: ((Exception) -> Unit)? = null
    var onCancelError: ((Exception) -> Unit)? = null

    fun findMatch(callback: (FindMatchResponse) -> Unit) {
        scope.launch {
            try {
                send<MatchMakingRequest>(FindMatchRequest(playerId))

                val response = receiveMessage<FindMatchResponse>()
                    ?: FindMatchResponse(false, "Нет ответа от сервера")

                Gdx.app.postRunnable { callback(response) }
            } catch (e: Exception) {
                Gdx.app.postRunnable {
                    onFindError?.invoke(e)
                    callback(FindMatchResponse(false, "Ошибка: ${e.message}"))
                }
            }
        }
    }

    fun cancelMatch(callback: (CancelMatchResponse) -> Unit) {
        scope.launch {
            try {
                send<MatchMakingRequest>(CancelMatchRequest(playerId))

                val response = receiveMessage<CancelMatchResponse>()
                    ?: CancelMatchResponse(false, "Нет ответа от сервера")

                Gdx.app.postRunnable { callback(response) }
            } catch (e: Exception) {
                Gdx.app.postRunnable {
                    onCancelError?.invoke(e)
                    callback(CancelMatchResponse(false, "Ошибка: ${e.message}"))
                }
            }
        }
    }
}