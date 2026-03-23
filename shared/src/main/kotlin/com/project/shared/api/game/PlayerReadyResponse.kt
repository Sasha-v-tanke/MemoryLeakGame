package com.project.shared.api.game

import com.project.shared.api.Response
import kotlinx.serialization.Serializable

@Serializable
data class PlayerReadyResponse(
    val success: Boolean,
    val description: String = "",
) : Response
