package com.project.shared.engine

import kotlinx.serialization.Serializable

@Serializable
data class GameStateSnapshot(
    val entities: List<EntityState>,
    val timestamp: Long
)