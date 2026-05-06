package com.project.shared.api.game

import com.project.shared.api.Response
import kotlinx.serialization.Serializable


@Serializable
sealed interface GameResponse : Response
