package com.project.client.api

import com.badlogic.gdx.Gdx
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import com.project.shared.api.*


class AuthWebSocket(private val url: String) {
    private val client = HttpClient { install(WebSockets) }
    private var session: DefaultClientWebSocketSession? = null
    private val wsScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun connect() {
        if (session != null) return
        session = client.webSocketSession(urlString = url)
    }

    private suspend fun sendRequest(request: AuthRequest): AuthResponse {
        return try {
            connect()
            val s = session ?: return AuthResponse(false, null, "WebSocket is not connected")

            s.send(Frame.Text(json.encodeToString(request)))

            val responseText: String = withTimeoutOrNull<String>(5_000) {
                while (true) {
                    val next = s.incoming.receive()
                    if (next is Frame.Text) return@withTimeoutOrNull next.readText()
                }
                ""
            } ?: return AuthResponse(false, null, "Server response timeout")

            json.decodeFromString<AuthResponse>(responseText)
        } catch (e: Exception) {
            Gdx.app.error("AuthWebSocket", "Request failed: ${e.message}", e)
            AuthResponse(false, null, "Connection error: ${e.message ?: "unknown"}")
        }
    }

    fun login(username: String, password: String, onResult: (AuthResponse) -> Unit) {
        wsScope.launch {
            val request = AuthRequest(username = username, password = password)
            val response = sendRequest(request)
            Gdx.app.postRunnable { onResult(response) }
        }
    }

    fun register(username: String, password: String, email: String, onResult: (AuthResponse) -> Unit) {
        wsScope.launch {
            val request = AuthRequest(username = username, password = password, email = email)
            val response = sendRequest(request)
            Gdx.app.postRunnable { onResult(response) }
        }
    }

    fun close() {
        wsScope.launch {
            session = null
            client.close()
        }
    }
}