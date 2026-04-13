package com.project.shared.engine.gameobjects

enum class UnitType {
    // Workers
    POINTER,
    ALLOCATOR,

    // Carriers
    BUFFER,
    QUEUE,

    // Fighters
    THREAD_POOL,
    COROUTINE,

    // Control
    DEADLOCK,
    MUTEX,

    // Support
    ENCAPSULATION,
    INHERITANCE,

    // Spells
    LAMBDA,
    GARBAGE_COLLECTOR
}