package com.project.shared.engine

import kotlinx.serialization.Serializable

sealed interface ComponentState

@Serializable
data class TransformComponent(
    val x: Float,
    val y: Float
) : ComponentState

@Serializable
data class SpriteComponent(
    val texture: String
) : ComponentState

@Serializable
data class HealthComponent(
    val hp: Int,
    val maxHp: Int
) : ComponentState