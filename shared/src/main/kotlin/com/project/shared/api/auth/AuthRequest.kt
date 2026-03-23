package com.project.shared.api.auth

import com.project.shared.api.Request
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthRequest : Request
