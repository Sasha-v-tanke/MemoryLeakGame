package com.project.shared.engine.config

import com.project.shared.engine.entities.OwnerType

data class EntityConfig(
    val owner: OwnerType,
    val x: Float,
    val y: Float,
    val scale: Float,
    val sprite: String
)