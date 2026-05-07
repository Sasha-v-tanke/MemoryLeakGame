package com.project.shared.engine

import com.project.shared.engine.entities.units.UnitType
import kotlinx.serialization.Serializable

@Serializable
data class MatchStats(
    val byPlayerId: Map<Int, PlayerMatchStats> = emptyMap()
)

@Serializable
data class PlayerMatchStats(
    val unitsQueued: Map<UnitType, Int> = emptyMap(),
    val unitsProduced: Map<UnitType, Int> = emptyMap(),
    val unitsLost: Map<UnitType, Int> = emptyMap(),
    val enemyUnitsKilled: Map<UnitType, Int> = emptyMap(),
    val memoryAllocated: Int = 0,
    val memoryFreed: Int = 0,
    val factoriesBuilt: Int = 0,
    val spellsCast: Map<UnitType, Int> = emptyMap()
)
