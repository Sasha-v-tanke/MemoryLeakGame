package com.project.client.network.api

import com.project.shared.api.JsonFormats
import com.project.shared.api.Request
import com.project.shared.api.Response
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString

abstract class WebSocket(private val endpoint: String) {
    private val baseUrl = System.getenv("MEMORY_LEAK_SERVER_WS")
        ?: "ws://localhost:8080"

    protected val client = HttpClient {
        install(WebSockets)
    }

    protected var session: DefaultClientWebSocketSession? = null
    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val json = JsonFormats.default
    protected val timeout = 5_000L

    protected val connectMutex = Mutex()
    protected val sendMutex = Mutex()

    open suspend fun connect() {
        connectMutex.withLock {
            if (session != null) return

            val normalizedEndpoint = if (endpoint.startsWith("/")) endpoint else "/$endpoint"
            session = client.webSocketSession(urlString = baseUrl + normalizedEndpoint)
        }
    }

    protected suspend inline fun <reified T : Request> send(request: T) {
        connect()

        sendMutex.withLock {
            val s = session ?: error("WebSocket is not connected")
            s.send(Frame.Text(json.encodeToString<T>(request)))
        }
    }

    protected suspend inline fun <reified T : Response> receiveMessage(): T? {
        val s = session ?: return null

        val responseText = withTimeoutOrNull(timeout) {
            while (true) {
                val next = s.incoming.receive()
                if (next is Frame.Text) {
                    return@withTimeoutOrNull next.readText()
                }
            }

            @Suppress("UNREACHABLE_CODE")
            ""
        } ?: return null

        return try {
            json.decodeFromString<T>(responseText)
        } catch (_: Exception) {
            null
        }
    }

    open fun close() {
        try {
            kotlinx.coroutines.runBlocking {
                session?.close()
            }
        } catch (_: Exception) {
        }

        session = null
        client.close()
        scope.cancel()
    }
}
