package com.project.shared.api.events

import com.project.shared.engine.EntityState
import kotlinx.serialization.Serializable

@Serializable
data class GameStateSnapshotEvent(
    val entities: List<EntityState>,
    val timestamp: Long
) : Event
