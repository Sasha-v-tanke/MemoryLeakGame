package com.project.shared.engine.entities.units

import kotlinx.serialization.Serializable

@Serializable
data class UnitConfig(
    val unitType: UnitType,
    val displayName: String,
    val role: UnitRole,
    val buildTime: Float,
    val health: Int,
    val speed: Float,
    val damage: Int,
    val attackRange: Float,
    val attackCooldownMillis: Long,
    val costCpu: Int,
    val costMemory: Int,
    val allocatedMemory: Int,
    val memoryWorkPower: Float,
    val cpuRedirectPower: Float,
    val sprite: String,
    val gameDescription: String,
    val techDescription: String,
    val strengths: String,
    val weaknesses: String,
    val realFeature: String
)
