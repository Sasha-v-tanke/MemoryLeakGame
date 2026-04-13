package com.project.shared.engine.units

import kotlinx.serialization.Serializable

@Serializable
enum class UnitRole {
    CAPTURE,
    SUPPORT,
    DEFENSE,
    ATTACK,
    SPECIAL
}