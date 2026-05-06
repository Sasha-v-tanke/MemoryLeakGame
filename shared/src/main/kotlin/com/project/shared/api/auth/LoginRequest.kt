package com.project.shared.api.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("login")
data class LoginRequest(
    val username: String,
    val password: String
) : AuthRequest
