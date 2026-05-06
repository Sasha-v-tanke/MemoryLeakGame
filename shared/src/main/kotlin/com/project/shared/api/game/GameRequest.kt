package com.project.shared.api.game

import com.project.shared.api.Request
import kotlinx.serialization.Serializable

@Serializable
sealed interface GameRequest : Request
