package com.project.shared.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("listen_ready")
data class ListenReadyRequest(
    val playerId: Int
) : Request
