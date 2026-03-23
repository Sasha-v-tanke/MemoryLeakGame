package com.project.shared.engine

import kotlinx.serialization.Serializable

@Serializable
enum class OwnerType {
    PLAYER_1,
    PLAYER_2,
    WORLD
}