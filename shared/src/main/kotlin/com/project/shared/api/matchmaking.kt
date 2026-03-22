package com.project.shared.api

import kotlinx.serialization.Serializable

@Serializable
data class FindMatchRequest(val playerId: Int, val create: Boolean)

@Serializable
data class FindMatchResponse(val success: Boolean, val type: String = "Error_find")

@Serializable
data class CancelMatchResponse(val success: Boolean, val type: String = "Error_cancel")

@Serializable
data class MatchFound(val roomId: String, val opponentId: Int, val index: Int, val type: String = "Error")

@Serializable
data class ErrorResponse(val success: Boolean, val type: String = "error")