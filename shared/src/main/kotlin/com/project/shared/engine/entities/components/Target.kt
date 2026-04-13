package com.project.shared.engine.entities.components

import kotlinx.serialization.Serializable

@Serializable
data class Target(
    var targetEntityId: Long? = null
) : Component