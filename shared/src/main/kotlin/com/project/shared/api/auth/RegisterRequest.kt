package com.project.shared.api.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("register")
data class RegisterRequest(
    val username: String,
    val password: String,
    val email: String
) : AuthRequest
