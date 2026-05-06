package com.project.shared.api.matchmaking

import com.project.shared.api.Response
import kotlinx.serialization.Serializable

@Serializable
sealed interface MatchMakingResponse : Response
