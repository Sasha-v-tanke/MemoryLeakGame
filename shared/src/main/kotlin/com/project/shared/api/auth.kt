package com.project.shared.api

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val username: String,
    val password: String,
    val email: String? = null
)

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val playerId: Int? = -1,
    val message: String
)