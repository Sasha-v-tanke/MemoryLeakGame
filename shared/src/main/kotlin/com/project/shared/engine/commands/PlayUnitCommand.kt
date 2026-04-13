package com.project.shared.engine.commands

import com.project.shared.engine.entities.units.UnitType
import kotlinx.serialization.Serializable

@Serializable
data class PlayUnitCommand(
    val playerId: Int,
    val unitType: UnitType,
    val targetX: Float,
    val targetY: Float
) : Command