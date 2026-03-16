package com.project.server.routing

import kotlinx.serialization.Serializable


@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val message: String
)