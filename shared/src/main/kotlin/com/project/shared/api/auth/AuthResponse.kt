package com.project.shared.api.auth

import com.project.shared.api.Response
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("auth_response")
data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val playerId: Int? = null,
    val message: String = ""
) : Response
