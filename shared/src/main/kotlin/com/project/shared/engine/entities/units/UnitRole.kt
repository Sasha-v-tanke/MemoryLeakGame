package com.project.shared.engine.entities.units

import kotlinx.serialization.Serializable

@Serializable
enum class UnitRole {
    CAPTURE,
    SUPPORT,
    DEFENSE,
    ATTACK,
    SPELL
}
