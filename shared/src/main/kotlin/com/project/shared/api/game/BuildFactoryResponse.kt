package com.project.shared.api.game

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("build_factory_response")
data class BuildFactoryResponse(
    val success: Boolean,
    val description: String = ""
) : GameResponse
