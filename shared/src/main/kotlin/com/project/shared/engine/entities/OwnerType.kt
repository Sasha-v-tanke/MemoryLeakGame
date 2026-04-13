package com.project.shared.engine.entities

import kotlinx.serialization.Serializable

@Serializable
enum class OwnerType {
    PLAYER_1,
    PLAYER_2,
    WORLD
}