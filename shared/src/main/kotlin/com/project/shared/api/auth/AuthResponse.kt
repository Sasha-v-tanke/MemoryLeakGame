package com.project.shared.api.auth

import com.project.shared.api.Response
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val playerId: Int? = -1,
    val message: String
) : Response