package com.project.shared.api.matchmaking

import com.project.shared.api.Request
import kotlinx.serialization.Serializable

@Serializable
sealed interface MatchMakingRequest : Request
