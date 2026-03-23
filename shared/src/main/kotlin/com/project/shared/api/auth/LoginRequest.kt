package com.project.shared.api.auth

import com.project.shared.api.Request
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
) : AuthRequest