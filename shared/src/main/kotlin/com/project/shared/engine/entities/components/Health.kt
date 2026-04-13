package com.project.shared.engine.entities.components

import kotlinx.serialization.Serializable

@Serializable
data class Health(
    var current: Int,
    val max: Int
) : Component