package com.project.shared.engine.entities.components

import kotlinx.serialization.Serializable

@Serializable
data class Sprite(val textureId: String, val scale: Float) : Component