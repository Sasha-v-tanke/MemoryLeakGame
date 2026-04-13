package com.project.shared.engine.gameobjects.components

import kotlinx.serialization.Serializable

@Serializable
data class Target(
    var targetEntityId: Long? = null
) : Component