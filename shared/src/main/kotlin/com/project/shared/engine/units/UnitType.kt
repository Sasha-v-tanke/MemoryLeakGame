package com.project.shared.engine.units

import kotlinx.serialization.Serializable

@Serializable
enum class UnitType {
    ALLOCATOR,
    GARBAGE_COLLECTOR,
    THREAD_GUARD,
    INJECTOR,
    DEADLOCK
}