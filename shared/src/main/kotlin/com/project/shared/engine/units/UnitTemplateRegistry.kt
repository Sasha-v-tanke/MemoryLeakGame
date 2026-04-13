package com.project.shared.engine.units

object UnitTemplateRegistry {
    val allocator = UnitTemplate(
        type = UnitType.ALLOCATOR,
        role = UnitRole.CAPTURE,
        sprite = "allocator.png",
        costMemory = 1,
        costCpu = 0,
        maxHealth = 30,
        damage = 2,
        speed = 240f,
        range = 35f
    )

    val garbageCollector = UnitTemplate(
        type = UnitType.GARBAGE_COLLECTOR,
        role = UnitRole.SUPPORT,
        sprite = "garbage_collector.png",
        costMemory = 2,
        costCpu = 1,
        maxHealth = 45,
        damage = 1,
        speed = 180f,
        range = 40f
    )

    val threadGuard = UnitTemplate(
        type = UnitType.THREAD_GUARD,
        role = UnitRole.DEFENSE,
        sprite = "thread_guard.png",
        costMemory = 2,
        costCpu = 1,
        maxHealth = 70,
        damage = 4,
        speed = 140f,
        range = 45f
    )

    val injector = UnitTemplate(
        type = UnitType.INJECTOR,
        role = UnitRole.ATTACK,
        sprite = "injector.png",
        costMemory = 3,
        costCpu = 1,
        maxHealth = 50,
        damage = 8,
        speed = 200f,
        range = 55f
    )

    val deadlock = UnitTemplate(
        type = UnitType.DEADLOCK,
        role = UnitRole.SPECIAL,
        sprite = "deadlock.png",
        costMemory = 4,
        costCpu = 2,
        maxHealth = 40,
        damage = 0,
        speed = 120f,
        range = 80f
    )

    val all = listOf(
        allocator,
        garbageCollector,
        threadGuard,
        injector,
        deadlock
    )

    fun find(type: UnitType): UnitTemplate? = all.firstOrNull { it.type == type }
}