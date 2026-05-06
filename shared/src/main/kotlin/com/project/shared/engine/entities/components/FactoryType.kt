package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FactoryType {
    BASIC,
    SUPPORT
}

@Serializable
@SerialName("factory")
data class Factory(
    val factoryType: FactoryType,
    var productionMultiplier: Float = 1f
) : Component
