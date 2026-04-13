package com.project.shared.engine

import com.project.shared.engine.entities.components.Component
import com.project.shared.engine.entities.OwnerType
import kotlinx.serialization.Serializable

@Serializable
data class EntityState(
    val id: Long,
    val owner: OwnerType?,
    val components: List<Component>
)
