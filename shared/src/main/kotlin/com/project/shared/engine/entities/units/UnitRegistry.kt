package com.project.shared.engine.entities.units

object UnitRegistry {
    private val units = mapOf(
        UnitType.ALLOCATOR to UnitConfig(
            unitType = UnitType.ALLOCATOR,
            displayName = "Allocator",
            role = UnitRole.CAPTURE,
            buildTime = 1.5f,
            health = 42,
            speed = 135f,
            damage = 8,
            attackRange = 52f,
            attackCooldownMillis = 850L,
            costCpu = 3,
            costMemory = 4,
            sprite = "units/allocator.png",
            gameDescription = "Fast cheap capturer. Best for taking Memory and CPU nodes early.",
            techDescription = "Allocator represents memory allocation: a basic operation that reserves memory for active work."
        ),

        UnitType.GARBAGE_COLLECTOR to UnitConfig(
            unitType = UnitType.GARBAGE_COLLECTOR,
            displayName = "Garbage Collector",
            role = UnitRole.SUPPORT,
            buildTime = 2.6f,
            health = 70,
            speed = 92f,
            damage = 4,
            attackRange = 120f,
            attackCooldownMillis = 1100L,
            costCpu = 4,
            costMemory = 8,
            sprite = "units/garbage_collector.png",
            gameDescription = "Support unit. Repairs nearby allied units and helps stabilize captured territory.",
            techDescription = "Garbage collection frees unused memory and keeps a system healthy over time."
        ),

        UnitType.THREAD_GUARD to UnitConfig(
            unitType = UnitType.THREAD_GUARD,
            displayName = "Thread Guard",
            role = UnitRole.DEFENSE,
            buildTime = 3.0f,
            health = 135,
            speed = 68f,
            damage = 14,
            attackRange = 70f,
            attackCooldownMillis = 950L,
            costCpu = 6,
            costMemory = 10,
            sprite = "units/thread_guard.png",
            gameDescription = "Durable defender. Holds nodes, factories and approaches to the Core.",
            techDescription = "Thread Guard represents guarded execution and synchronization around critical system resources."
        ),

        UnitType.INJECTOR to UnitConfig(
            unitType = UnitType.INJECTOR,
            displayName = "Injector",
            role = UnitRole.ATTACK,
            buildTime = 2.2f,
            health = 78,
            speed = 112f,
            damage = 24,
            attackRange = 64f,
            attackCooldownMillis = 780L,
            costCpu = 7,
            costMemory = 11,
            sprite = "units/injector.png",
            gameDescription = "Aggressive attacker. Strong against factories and the enemy Core if protected.",
            techDescription = "Injector is a metaphor for code or dependency injection: powerful, direct and risky."
        ),

        UnitType.DEADLOCK to UnitConfig(
            unitType = UnitType.DEADLOCK,
            displayName = "Deadlock",
            role = UnitRole.SPELL,
            buildTime = 0.2f,
            health = 1,
            speed = 0f,
            damage = 0,
            attackRange = 145f,
            attackCooldownMillis = 0L,
            costCpu = 8,
            costMemory = 10,
            sprite = "units/deadlock.png",
            gameDescription = "Control spell. Temporarily stuns enemies in the selected area.",
            techDescription = "Deadlock is a state where processes wait forever for each other’s resources."
        ),

        UnitType.OVERCLOCK to UnitConfig(
            unitType = UnitType.OVERCLOCK,
            displayName = "Overclock",
            role = UnitRole.SPELL,
            buildTime = 0.2f,
            health = 1,
            speed = 0f,
            damage = 0,
            attackRange = 150f,
            attackCooldownMillis = 0L,
            costCpu = 7,
            costMemory = 8,
            sprite = "units/overclock.png",
            gameDescription = "Buff spell. Temporarily speeds allied units in the selected area.",
            techDescription = "Overclocking increases performance above the default operating rate, usually with extra risk or cost."
        ),

        UnitType.CACHE_RUNNER to UnitConfig(
            unitType = UnitType.CACHE_RUNNER,
            displayName = "Cache Runner",
            role = UnitRole.CAPTURE,
            buildTime = 1.0f,
            health = 28,
            speed = 172f,
            damage = 5,
            attackRange = 48f,
            attackCooldownMillis = 720L,
            costCpu = 3,
            costMemory = 4,
            sprite = "units/cache_runner.png",
            gameDescription = "Ultra-fast capturer. Reaches neutral nodes first but collapses quickly under pressure.",
            techDescription = "Cache Runner represents cache locality: fast access gives tempo, but cached state is small and fragile."
        ),

        UnitType.FIREWALL to UnitConfig(
            unitType = UnitType.FIREWALL,
            displayName = "Firewall",
            role = UnitRole.DEFENSE,
            buildTime = 3.4f,
            health = 165,
            speed = 48f,
            damage = 11,
            attackRange = 82f,
            attackCooldownMillis = 1050L,
            costCpu = 8,
            costMemory = 14,
            sprite = "units/firewall.png",
            gameDescription = "Slow defensive wall. Protects factories, nodes and Core approaches.",
            techDescription = "Firewall models a boundary that filters hostile traffic before it reaches critical system parts."
        ),

        UnitType.COROUTINE_ARCHER to UnitConfig(
            unitType = UnitType.COROUTINE_ARCHER,
            displayName = "Coroutine Archer",
            role = UnitRole.ATTACK,
            buildTime = 2.8f,
            health = 52,
            speed = 104f,
            damage = 18,
            attackRange = 165f,
            attackCooldownMillis = 920L,
            costCpu = 7,
            costMemory = 10,
            sprite = "units/coroutine_archer.png",
            gameDescription = "Long-range attacker. Deals damage from a safe distance but needs protection.",
            techDescription = "Coroutine Archer shows asynchronous execution: useful work continues without blocking the whole system."
        ),

        UnitType.PATCH_HEALER to UnitConfig(
            unitType = UnitType.PATCH_HEALER,
            displayName = "Patch Healer",
            role = UnitRole.SUPPORT,
            buildTime = 2.4f,
            health = 58,
            speed = 98f,
            damage = 2,
            attackRange = 112f,
            attackCooldownMillis = 1250L,
            costCpu = 4,
            costMemory = 7,
            sprite = "units/patch_healer.png",
            gameDescription = "Cheap support process. Repairs nearby allies and keeps a push alive.",
            techDescription = "Patch Healer represents hotfixes and maintenance patches that restore stability during operation."
        )
    )

    val defaultDeck: List<UnitType> = listOf(
        UnitType.ALLOCATOR,
        UnitType.GARBAGE_COLLECTOR,
        UnitType.THREAD_GUARD,
        UnitType.INJECTOR,
        UnitType.DEADLOCK,
        UnitType.OVERCLOCK
    )

    fun getConfig(type: UnitType): UnitConfig {
        return units[type] ?: error("No UnitConfig registered for $type")
    }

    fun all(): List<UnitConfig> {
        return units.values.toList()
    }
}
