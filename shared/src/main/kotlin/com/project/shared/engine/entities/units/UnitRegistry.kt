package com.project.shared.engine.entities.units

object UnitRegistry {
    private val units = mapOf(
        UnitType.ALLOCATOR to UnitConfig(
            unitType = UnitType.ALLOCATOR,
            buildTime = 2f,
            health = 24f,
            capacity = 1,
            speed = 1.2f,
            costCPU = 1,
            costRAM = 1,
            sprite = "units/allocator.png",
            role = UnitRole.CAPTURE
        ),
        UnitType.GARBAGE_COLLECTOR to UnitConfig(
            unitType = UnitType.GARBAGE_COLLECTOR,
            buildTime = 4f,
            health = 36f,
            capacity = 0,
            speed = 1.0f,
            costCPU = 2,
            costRAM = 3,
            sprite = "units/garbage_collector.png",
            role = UnitRole.SUPPORT
        ),
        UnitType.THREAD_GUARD to UnitConfig(
            unitType = UnitType.THREAD_GUARD,
            buildTime = 4f,
            health = 60f,
            capacity = 0,
            speed = 0.7f,
            costCPU = 3,
            costRAM = 3,
            sprite = "units/thread_guard.png",
            role = UnitRole.DEFENSE
        ),
        UnitType.INJECTOR to UnitConfig(
            unitType = UnitType.INJECTOR,
            buildTime = 3f,
            health = 40f,
            capacity = 0,
            speed = 1.1f,
            costCPU = 3,
            costRAM = 4,
            sprite = "units/injector.png",
            role = UnitRole.ATTACK
        ),
        UnitType.DEADLOCK to UnitConfig(
            unitType = UnitType.DEADLOCK,
            buildTime = 5f,
            health = 50f,
            capacity = 0,
            speed = 0.6f,
            costCPU = 4,
            costRAM = 5,
            sprite = "units/deadlock.png",
            role = UnitRole.SPECIAL
        )
    )

    fun getConfig(type: UnitType): UnitConfig =
        units[type] ?: error("No UnitConfig registered for $type")
}