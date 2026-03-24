package com.project.shared.api.matchmaking

import com.project.shared.api.Response
import kotlinx.serialization.Serializable

@Serializable
data class FindMatchResponse(
    val success: Boolean,
    val description: String = ""
) : Response