package com.project.shared.api.events

import com.project.shared.engine.EntityState
import com.project.shared.engine.PlayerResources
import com.project.shared.engine.entities.components.FactoryType
import com.project.shared.engine.entities.units.UnitType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("game_state_snapshot")
data class GameStateSnapshotEvent(
    val entities: List<EntityState>,
    val resources: Map<Int, PlayerResources>,
    val cardCooldownsMs: Map<Int, Map<UnitType, Long>>,
    val factoryQueueSizes: Map<Int, Map<FactoryType, Int>>,
    val timestamp: Long,
    val tick: Long
) : Event
