package com.project.client.network.api

import com.badlogic.gdx.Gdx
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString

import com.project.shared.api.*


class AuthWebSocket(endpoint: String) : WebSocket(endpoint) {
    private suspend fun sendRequest(request: AuthRequest): AuthResponse {
        return try {
            connect()
            val s = session ?: return AuthResponse(false, null, -1, "WebSocket is not connected")

            s.send(Frame.Text(json.encodeToString(request)))

            val responseText: String = withTimeoutOrNull(5_000) {
                while (true) {
                    val next = s.incoming.receive()
                    if (next is Frame.Text) return@withTimeoutOrNull next.readText()
                }
                ""
            } ?: return AuthResponse(false, null, -1, "Server response timeout")

            json.decodeFromString<AuthResponse>(responseText)
        } catch (e: Exception) {
            Gdx.app.error("AuthWebSocket", "Request failed: ${e.message}", e)
            AuthResponse(false, null, -1, "Connection error: ${e.message ?: "unknown"}")
        }
    }

    fun login(username: String, password: String, onResult: (AuthResponse) -> Unit) {
        scope.launch {
            val request = AuthRequest(username = username, password = password)
            val response = sendRequest(request)
            Gdx.app.postRunnable { onResult(response) }
        }
    }

    fun register(username: String, password: String, email: String, onResult: (AuthResponse) -> Unit) {
        scope.launch {
            val request = AuthRequest(username = username, password = password, email = email)
            val response = sendRequest(request)
            Gdx.app.postRunnable { onResult(response) }
        }
    }
}