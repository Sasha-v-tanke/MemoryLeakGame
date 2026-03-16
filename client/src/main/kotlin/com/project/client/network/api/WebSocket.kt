package com.project.client.network.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

abstract class WebSocket(private val endpoint: String) {
    private val baseUrl = "ws://localhost:8080"
    private val client = HttpClient { install(WebSockets) }
    protected var session: DefaultClientWebSocketSession? = null
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val json = Json { ignoreUnknownKeys = true }

    open suspend fun connect() {
        if (session != null) return
        session = client.webSocketSession(urlString = baseUrl + endpoint)
    }

    fun close() {
        scope.launch {
            session = null
            client.close()
        }
    }
}