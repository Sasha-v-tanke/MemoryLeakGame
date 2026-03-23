package com.project.shared.api.matchmaking

import kotlinx.serialization.Serializable


@Serializable
data class CancelMatchResponse(val success: Boolean, val type: String = "Error_cancel")