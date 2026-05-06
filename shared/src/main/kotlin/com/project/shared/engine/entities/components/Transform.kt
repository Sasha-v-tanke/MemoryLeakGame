package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("transform")
data class Transform(
    var x: Float,
    var y: Float
) : Component
