package com.project.shared.api.matchmaking

import com.project.shared.api.Request
import kotlinx.serialization.Serializable

@Serializable
data class FindMatchRequest(
    val playerId: Int,
    val create: Boolean
) : Request
