package com.project.client.network.api

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.launch

import com.project.shared.api.auth.*


class AuthSocket : WebSocket("auth") {
    private suspend fun sendRequest(request: AuthRequest): AuthResponse {
        return try {
            send(request)
            receiveMessage<AuthResponse>()
                ?: AuthResponse(false, null, -1, "No response received within timeout")
        } catch (e: Exception) {
            Gdx.app.error("AuthWebSocket", "Request failed: ${e.message}", e)
            AuthResponse(false, null, -1, "Connection error: ${e.message ?: "unknown"}")
        }
    }

    fun login(username: String, password: String, onResult: (AuthResponse) -> Unit) {
        scope.launch {
            val request = LoginRequest(username, password)
            val response = sendRequest(request)
            Gdx.app.postRunnable { onResult(response) }
        }
    }

    fun register(username: String, password: String, email: String, onResult: (AuthResponse) -> Unit) {
        scope.launch {
            val request = RegisterRequest(username, password, email)
            val response = sendRequest(request)
            Gdx.app.postRunnable { onResult(response) }
        }
    }
}