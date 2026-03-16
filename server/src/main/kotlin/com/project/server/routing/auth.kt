package com.project.server.routing

import com.project.server.models.User
import com.project.server.repository.UserRepository
import com.project.shared.api.AuthResponse
import com.project.shared.api.LoginRequest
import com.project.shared.api.RegisterRequest
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


fun Application.authModule() {
    routing {
        webSocket("/user/register") {
            val json = Json { ignoreUnknownKeys = true }
            incoming.consumeEach { frame ->
                if (frame !is Frame.Text) return@consumeEach
                try {
                    val req = json.decodeFromString<RegisterRequest>(frame.readText())
                    val response = if (UserRepository.findByUsername(req.username) != null) {
                        AuthResponse(false, null, -1, "Username already exists")
                    } else {
                        val newUser = UserRepository.addUser(
                            User(id = null, name = req.username, email = req.email, password = req.password)
                        )
                        AuthResponse(true, "dummy-token-${newUser.id}", -1, "User registered successfully")
                    }
                    outgoing.send(Frame.Text(json.encodeToString(response)))
                } catch (e: Exception) {
                    outgoing.send(Frame.Text(json.encodeToString(AuthResponse(false, null, -1, "Invalid request format"))))
                }
            }
        }

        webSocket("/user/login") {
            val json = Json { ignoreUnknownKeys = true }
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    try {
                        val req = json.decodeFromString<LoginRequest>(frame.readText())
                        val user = UserRepository.findByUsername(req.username)
                        val response = if (user == null || user.password != req.password) {
                            AuthResponse(false, null, -1, "Invalid username or password")
                        } else {
                            AuthResponse(true, "dummy-token-${user.id}", user.id, "Login successful")
                        }
                        outgoing.send(Frame.Text(json.encodeToString(response)))
                    } catch (e: Exception) {
                        outgoing.send(Frame.Text(json.encodeToString(AuthResponse(false, null, -1, "Invalid request format"))))
                    }
                }
            }
        }
    }
}
