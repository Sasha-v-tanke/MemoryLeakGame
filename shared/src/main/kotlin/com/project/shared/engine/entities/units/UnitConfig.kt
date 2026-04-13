package com.project.shared.engine.entities.units

import kotlinx.serialization.Serializable

@Serializable
data class UnitConfig(
    val unitType: UnitType,
    val buildTime: Float,
    val health: Float,
    val capacity: Int,
    val speed: Float,
    val costCPU: Int,
    val costRAM: Int,
    val sprite: String,
    val role: UnitRole
)