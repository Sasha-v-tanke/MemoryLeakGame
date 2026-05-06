package com.project.shared.api.game

import com.project.shared.engine.entities.units.UnitType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("play_card")
data class PlayCardRequest(
    val playerId: Int,
    val roomId: String,
    val unitType: UnitType,
    val targetX: Float,
    val targetY: Float
) : GameRequest
