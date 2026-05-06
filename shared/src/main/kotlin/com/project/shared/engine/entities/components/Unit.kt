package com.project.shared.engine.entities.components

import com.project.shared.engine.entities.units.UnitRole
import com.project.shared.engine.entities.units.UnitType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("unit")
data class Unit(
    val type: UnitType,
    val role: UnitRole,
    val costMemory: Int,
    val costCpu: Int
) : Component
