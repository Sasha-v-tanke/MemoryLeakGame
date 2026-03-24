package com.project.shared.api

import kotlinx.serialization.Serializable

@Serializable
data class ListenReadyRequest(
    val playerId: Int
) : Request