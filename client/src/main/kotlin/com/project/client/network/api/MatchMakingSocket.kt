package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.CancelMatchResponse
import com.project.shared.api.FindMatchRequest
import com.project.shared.api.FindMatchResponse
import com.project.shared.api.MatchFound
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

class MatchMakingSocket(endpoint: String, private val playerId: Int) : WebSocket(endpoint) {

    var onFindMatchResponse: ((FindMatchResponse) -> Unit)? = null
    var onMatchFound: ((MatchFound) -> Unit)? = null
    var onCancelMatchResponse: ((CancelMatchResponse) -> Unit)? = null
    var onError: ((Throwable) -> Unit)? = null

    override suspend fun connect() {
        super.connect()
        listenIncoming()
    }

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

    private fun listenIncoming() {
        scope.launch {
            try {
                session?.incoming?.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        dispatchMessage(text)
                    }
                }
            } catch (e: Exception) {
                Gdx.app.postRunnable { onError?.invoke(e) }
            }
        }
    }

    private fun dispatchMessage(text: String) {
        try {
            println(text)
            when {
                text.contains("FindMatchResponse") -> {
                    val resp = Json.decodeFromString<FindMatchResponse>(text)
                    Gdx.app.postRunnable { onFindMatchResponse?.invoke(resp) }
                }

                text.contains("CancelMatchResponse") -> {
                    val resp = Json.decodeFromString<CancelMatchResponse>(text)
                    Gdx.app.postRunnable { onCancelMatchResponse?.invoke(resp) }
                }

                text.contains("MatchFound") -> {
                    val found = Json.decodeFromString<MatchFound>(text)
                    Gdx.app.postRunnable { onMatchFound?.invoke(found) }
                }
            }
        } catch (e: Exception) {
            Gdx.app.postRunnable { onError?.invoke(e) }
        }
    }

}