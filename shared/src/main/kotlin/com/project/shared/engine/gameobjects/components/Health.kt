package com.project.shared.engine.gameobjects.components

import kotlinx.serialization.Serializable

@Serializable
data class Health(
    var current: Int,
    val max: Int
) : Component