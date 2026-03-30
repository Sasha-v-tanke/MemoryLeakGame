package com.project.shared.engine.gameobjects.components

import kotlinx.serialization.Serializable

@Serializable
data class Sprite(val textureId: String) : Component