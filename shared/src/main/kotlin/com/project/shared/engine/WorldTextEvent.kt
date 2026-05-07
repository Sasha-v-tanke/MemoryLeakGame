package com.project.shared.engine

import com.project.shared.engine.entities.OwnerType
import kotlinx.serialization.Serializable

@Serializable
data class WorldTextEvent(
    val id: Long,
    val owner: OwnerType,
    val x: Float,
    val y: Float,
    val text: String,
    val createdAt: Long,
    val ttlMillis: Long = 1800L
)
