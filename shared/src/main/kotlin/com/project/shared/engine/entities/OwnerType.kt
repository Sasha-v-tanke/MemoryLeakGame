package com.project.shared.engine.entities

import kotlinx.serialization.Serializable

@Serializable
enum class OwnerType {
    PLAYER_1,
    PLAYER_2,
    WORLD;

    fun playerIndexOrNull(): Int? {
        return when (this) {
            PLAYER_1 -> 1
            PLAYER_2 -> 2
            WORLD -> null
        }
    }

    fun isPlayer(): Boolean = this == PLAYER_1 || this == PLAYER_2

    fun opponent(): OwnerType {
        return when (this) {
            PLAYER_1 -> PLAYER_2
            PLAYER_2 -> PLAYER_1
            WORLD -> WORLD
        }
    }

    companion object {
        fun fromPlayerIndex(index: Int): OwnerType {
            return when (index) {
                1 -> PLAYER_1
                2 -> PLAYER_2
                else -> WORLD
            }
        }
    }
}
