package com.project.shared.engine.gameobjects

import com.project.shared.engine.OwnerType

data class ObjectConfig(
    val owner: OwnerType,
    val x: Float,
    val y: Float,
    val scale: Float,
    val sprite: String
)