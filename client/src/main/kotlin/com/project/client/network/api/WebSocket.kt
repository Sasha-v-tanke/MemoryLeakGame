package com.project.client.network.api

import com.project.shared.api.Message
import com.project.shared.api.Request
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

abstract class WebSocket(private val endpoint: String) {
    private val baseUrl = "ws://localhost:8080"
    private val client = HttpClient { install(WebSockets) }
    protected var session: DefaultClientWebSocketSession? = null
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        encodeDefaults = true
    }
    protected val timeout = 5_000L

    open fun connect() {
        if (session != null) return
        val normalized = when {
            endpoint.startsWith("/") -> endpoint
            else -> "/$endpoint"
        }
        scope.launch {
            session = client.webSocketSession(urlString = baseUrl + normalized)
        }
    }


    protected suspend inline fun <reified T : Request> send(request: T) {
        connect()
        val s = session ?: error("WebSocket not connected")
        s.send(Frame.Text(json.encodeToString(request)))
    }


    protected suspend inline fun <reified T : Message> receiveMessage(): T? {
        val s = session ?: return null
        val responseText: String = withTimeoutOrNull(timeout) {
            while (true) {
                val next = s.incoming.receive()
                if (next is Frame.Text) return@withTimeoutOrNull next.readText()
            }
            @Suppress("UNREACHABLE_CODE")
            ""
        } ?: return null

        return try {
            json.decodeFromString<T>(responseText)
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        scope.launch {
            session = null
            client.close()
        }
    }
}