package com.project.shared.api.game

import com.project.shared.engine.entities.components.FactoryType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("build_factory")
data class BuildFactoryRequest(
    val playerId: Int,
    val roomId: String,
    val factoryType: FactoryType
) : GameRequest
