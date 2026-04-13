package com.project.shared.engine.gameobjects

object UnitRegistry {
    private val units = mapOf(
        UnitType.POINTER to UnitConfig(
            unitType = UnitType.POINTER,
            buildTime = 2f,
            health = 20f,
            capacity = 1,
            speed = 1.0f,
            costCPU = 1,
            costRAM = 1,
            sprite = "units/pointer.png",
            role = UnitRole.WORKER
        ),
        UnitType.ALLOCATOR to UnitConfig(
            unitType = UnitType.ALLOCATOR,
            buildTime = 3f,
            health = 26f,
            capacity = 1,
            speed = 0.9f,
            costCPU = 2,
            costRAM = 2,
            sprite = "units/allocator.png",
            role = UnitRole.WORKER
        ),
        UnitType.BUFFER to UnitConfig(
            unitType = UnitType.BUFFER,
            buildTime = 3f,
            health = 50f,
            capacity = 2,
            speed = 0.7f,
            costCPU = 4,
            costRAM = 12,
            sprite = "units/buffer.png",
            role = UnitRole.CARRIER
        ),
        UnitType.QUEUE to UnitConfig(
            unitType = UnitType.QUEUE,
            buildTime = 4f,
            health = 42f,
            capacity = 3,
            speed = 0.75f,
            costCPU = 5,
            costRAM = 10,
            sprite = "units/queue.png",
            role = UnitRole.CARRIER
        ),
        UnitType.THREAD_POOL to UnitConfig(
            unitType = UnitType.THREAD_POOL,
            buildTime = 5f,
            health = 65f,
            capacity = 2,
            speed = 0.95f,
            costCPU = 6,
            costRAM = 8,
            sprite = "units/thread_pool.png",
            role = UnitRole.FIGHTER
        ),
        UnitType.COROUTINE to UnitConfig(
            unitType = UnitType.COROUTINE,
            buildTime = 4f,
            health = 38f,
            capacity = 1,
            speed = 1.3f,
            costCPU = 4,
            costRAM = 6,
            sprite = "units/coroutine.png",
            role = UnitRole.FIGHTER
        ),
        UnitType.DEADLOCK to UnitConfig(
            unitType = UnitType.DEADLOCK,
            buildTime = 6f,
            health = 80f,
            capacity = 0,
            speed = 0.4f,
            costCPU = 8,
            costRAM = 8,
            sprite = "units/deadlock.png",
            role = UnitRole.CONTROL
        ),
        UnitType.MUTEX to UnitConfig(
            unitType = UnitType.MUTEX,
            buildTime = 5f,
            health = 70f,
            capacity = 0,
            speed = 0.5f,
            costCPU = 6,
            costRAM = 5,
            sprite = "units/mutex.png",
            role = UnitRole.CONTROL
        ),
        UnitType.ENCAPSULATION to UnitConfig(
            unitType = UnitType.ENCAPSULATION,
            buildTime = 4f,
            health = 45f,
            capacity = 1,
            speed = 0.85f,
            costCPU = 3,
            costRAM = 5,
            sprite = "units/encapsulation.png",
            role = UnitRole.SUPPORT
        ),
        UnitType.INHERITANCE to UnitConfig(
            unitType = UnitType.INHERITANCE,
            buildTime = 4f,
            health = 48f,
            capacity = 1,
            speed = 0.8f,
            costCPU = 3,
            costRAM = 6,
            sprite = "units/inheritance.png",
            role = UnitRole.SUPPORT
        ),
        UnitType.LAMBDA to UnitConfig(
            unitType = UnitType.LAMBDA,
            buildTime = 3f,
            health = 22f,
            capacity = 0,
            speed = 1.4f,
            costCPU = 2,
            costRAM = 4,
            sprite = "units/lambda.png",
            role = UnitRole.SPELL
        ),
        UnitType.GARBAGE_COLLECTOR to UnitConfig(
            unitType = UnitType.GARBAGE_COLLECTOR,
            buildTime = 6f,
            health = 55f,
            capacity = 0,
            speed = 0.9f,
            costCPU = 5,
            costRAM = 7,
            sprite = "units/garbage_collector.png",
            role = UnitRole.SPELL
        )
    )

    fun getConfig(type: UnitType): UnitConfig =
        units[type] ?: error("No UnitConfig registered for $type")
}