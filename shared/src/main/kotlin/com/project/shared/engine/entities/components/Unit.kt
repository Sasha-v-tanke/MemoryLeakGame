package com.project.shared.engine.entities.components

import com.project.shared.engine.entities.units.UnitRole
import com.project.shared.engine.entities.units.UnitType
import kotlinx.serialization.Serializable

@Serializable
data class Unit(
    val type: UnitType,
    val role: UnitRole,
    val costMemory: Int,
    val costCpu: Int,
    val maxHealth: Int,
    val damage: Int,
    val speed: Float,
    val range: Float
) : Component