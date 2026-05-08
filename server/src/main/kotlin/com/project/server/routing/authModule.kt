package com.project.server.routing

import com.project.server.models.PlayerSession
import com.project.server.models.User
import com.project.server.repository.UserRepository
import com.project.server.service.MatchQueue
import com.project.server.service.SessionManager
import com.project.shared.api.JsonFormats
import com.project.shared.api.ListenReadyRequest
import com.project.shared.api.auth.AuthRequest
import com.project.shared.api.auth.AuthResponse
import com.project.shared.api.auth.LoginRequest
import com.project.shared.api.auth.RegisterRequest
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.server.websocket.application
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import java.util.UUID

fun Application.authModule() {
    val json = JsonFormats.default

    routing {
        webSocket("/auth") {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach

                val response = try {
                    when (val request = json.decodeFromString<AuthRequest>(frame.readText())) {
                        is RegisterRequest -> register(request)
                        is LoginRequest -> login(request)
                    }
                } catch (e: Exception) {
                    AuthResponse(
                        success = false,
                        token = null,
                        playerId = null,
                        message = "Неверный запрос аутентификации: ${e.message}"
                    )
                }

                outgoing.send(Frame.Text(json.encodeToString(response)))
            }
        }

        webSocket("/listen") {
            var playerSession: PlayerSession? = null

            try {
                incoming.consumeEach { frame ->
                    if (frame !is Frame.Text) return@consumeEach

                    try {
                        val request = json.decodeFromString<ListenReadyRequest>(frame.readText())
                        val sessionId = UUID.randomUUID().toString()

                        playerSession = PlayerSession(
                            sessionId = sessionId,
                            playerId = request.playerId,
                            socket = this
                        )

                        SessionManager.addSession(playerSession!!)
                        application.log.info("Player ${request.playerId} connected to listen socket")
                    } catch (e: Exception) {
                        application.log.error("Listen socket error", e)
                    }
                }
            } finally {
                playerSession?.let { session ->
                    application.log.info("Player ${session.playerId} disconnected from listen socket")
                    MatchQueue.removePlayer(session.playerId)
                    SessionManager.removeSession(session.sessionId)
                }
            }
        }
    }
}

fun register(request: RegisterRequest): AuthResponse {
    val username = request.username.trim()
    val email = request.email.trim()
    val password = request.password

    if (username.length < 3) {
        return AuthResponse(false, null, null, "Имя пользователя должно содержать не менее 3 символов")
    }

    if (password.length < 4) {
        return AuthResponse(false, null, null, "Пароль должен содержать не менее 4 символов")
    }

    if (!email.contains("@")) {
        return AuthResponse(false, null, null, "Неверный email")
    }

    if (UserRepository.findByUsername(username) != null) {
        return AuthResponse(false, null, null, "Имя пользователя уже существует")
    }

    val newUser = UserRepository.addUser(
        User(
            id = null,
            name = username,
            email = email,
            password = password
        )
    )

    return AuthResponse(
        success = true,
        token = "dummy-token-${newUser.id}",
        playerId = newUser.id,
        message = "Пользователь успешно зарегистрирован"
    )
}

fun login(request: LoginRequest): AuthResponse {
    val username = request.username.trim()
    val user = UserRepository.findByUsername(username)

    if (user == null || user.password != request.password) {
        return AuthResponse(false, null, null, "Неверное имя пользователя или пароль")
    }

    return AuthResponse(
        success = true,
        token = "dummy-token-${user.id}",
        playerId = user.id,
        message = "Вход выполнен успешно"
    )
}
