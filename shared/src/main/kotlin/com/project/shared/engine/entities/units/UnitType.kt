package com.project.shared.engine.entities.units

import kotlinx.serialization.Serializable

@Serializable
enum class UnitType {
    ALLOCATOR,
    GARBAGE_COLLECTOR,
    THREAD_GUARD,
    INJECTOR,
    DEADLOCK,
    OVERCLOCK
}
