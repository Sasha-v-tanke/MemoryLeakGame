package com.project.server.routing

import com.project.server.models.PlayerSession
import com.project.server.models.User
import com.project.server.repository.UserRepository
import com.project.server.service.SessionManager
import com.project.shared.api.auth.AuthRequest
import com.project.shared.api.auth.AuthResponse
import com.project.shared.api.auth.ListenReadyRequest
import com.project.shared.api.auth.LoginRequest
import com.project.shared.api.auth.RegisterRequest
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID


fun Application.authModule() {
    val json = Json { ignoreUnknownKeys = true }
    routing {
        webSocket("/auth") {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach

                try {
                    val request = json.decodeFromString<AuthRequest>(frame.readText())

                    when (request) {
                        is RegisterRequest -> {
                            val response = register(request)
                            outgoing.send(Frame.Text(json.encodeToString(response)))
                        }

                        is LoginRequest -> {
                            val response = login(request)
                            outgoing.send(Frame.Text(json.encodeToString(response)))
                        }

                        is ListenReadyRequest -> {
                            val sessionId = UUID.randomUUID().toString()
                            val playerSession = PlayerSession(sessionId, request.playerId, this)
                            SessionManager.addSession(playerSession)
                        }
                    }
                } catch (e: Exception) {
                    outgoing.send(Frame.Text(json.encodeToString(AuthResponse(false, null, -1, "Invalid request format"))))
                }
            }
        }
    }
}

fun register(request: RegisterRequest): AuthResponse {
    return if (UserRepository.findByUsername(request.username) != null) {
        AuthResponse(false, null, -1, "Username already exists")
    } else {
        val newUser = UserRepository.addUser(
            User(id = null, name = request.username, email = request.email, password = request.password)
        )
        AuthResponse(true, "dummy-token-${newUser.id}", -1, "User registered successfully")
    }
}

fun login(request: LoginRequest): AuthResponse {
    val user = UserRepository.findByUsername(request.username)
    return if (user == null || user.password != request.password) {
        AuthResponse(false, null, -1, "Invalid username or password")
    } else {
        AuthResponse(true, "dummy-token-${user.id}", user.id, "Login successful")
    }
}
