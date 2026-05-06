package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("target")
data class Target(
    var targetEntityId: Long? = null,
    var targetX: Float? = null,
    var targetY: Float? = null
) : Component
