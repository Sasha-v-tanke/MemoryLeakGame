package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.auth.AuthRequest
import com.project.shared.api.auth.AuthResponse
import com.project.shared.api.auth.LoginRequest
import com.project.shared.api.auth.RegisterRequest
import kotlinx.coroutines.launch

class AuthSocket : WebSocket("auth") {
    private suspend fun sendRequest(request: AuthRequest): AuthResponse {
        return try {
            send<AuthRequest>(request)

            receiveMessage<AuthResponse>()
                ?: AuthResponse(false, null, null, "No response received")
        } catch (e: Exception) {
            Gdx.app.error("AuthSocket", "Request failed: ${e.message}", e)
            AuthResponse(false, null, null, "Connection error: ${e.message ?: "unknown"}")
        }
    }

    fun login(username: String, password: String, onResult: (AuthResponse) -> Unit) {
        scope.launch {
            val response = sendRequest(LoginRequest(username, password))
            Gdx.app.postRunnable { onResult(response) }
        }
    }

    fun register(username: String, password: String, email: String, onResult: (AuthResponse) -> Unit) {
        scope.launch {
            val response = sendRequest(RegisterRequest(username, password, email))
            Gdx.app.postRunnable { onResult(response) }
        }
    }
}
