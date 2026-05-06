package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("sprite")
data class Sprite(
    val textureId: String,
    val scale: Float = 1f
) : Component
