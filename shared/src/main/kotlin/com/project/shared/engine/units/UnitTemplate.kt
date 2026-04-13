package com.project.shared.engine.units

data class UnitTemplate(
    val type: UnitType,
    val role: UnitRole,
    val sprite: String,
    val costMemory: Int,
    val costCpu: Int,
    val maxHealth: Int,
    val damage: Int,
    val speed: Float,
    val range: Float
)