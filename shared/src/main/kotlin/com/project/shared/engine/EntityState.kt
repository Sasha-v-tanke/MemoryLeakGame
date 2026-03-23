package com.project.shared.engine

import kotlinx.serialization.Serializable

@Serializable
data class EntityState(
    val id: Long,
    val owner: OwnerType?,
    val components: List<ComponentState>
)
