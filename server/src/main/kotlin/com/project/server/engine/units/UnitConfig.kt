package com.project.server.engine.units

import com.project.shared.engine.entities.units.UnitType
import com.project.shared.engine.entities.components.FactoryType

/**
 * Конфигурация для юнитов: кулдауны, требования, батчи памяти, тексты
 */
object UnitConfig {

    fun cooldownMillisFor(unitType: UnitType): Long {
        return when (unitType) {
            UnitType.ALLOCATOR -> 2200L
            UnitType.BUFFER -> 3200L
            UnitType.MEMORY_POOL -> 5200L
            UnitType.DMA_CONTROLLER -> 4200L
            UnitType.CPU_SCHEDULER -> 2800L
            UnitType.LOAD_BALANCER -> 5600L
            UnitType.INTERRUPT_HANDLER -> 3000L
            UnitType.CACHE_RUNNER -> 3000L
            UnitType.GARBAGE_COLLECTOR -> 5200L
            UnitType.PATCH_HEALER -> 4500L
            UnitType.THREAD_GUARD -> 6300L
            UnitType.FIREWALL -> 7600L
            UnitType.INJECTOR -> 6800L
            UnitType.COROUTINE_ARCHER -> 6400L
            UnitType.DEADLOCK -> 10000L
            UnitType.OVERCLOCK -> 8500L
            UnitType.STACK_FRAME -> 2800L
            UnitType.HEAP_BLOCK -> 4000L
            UnitType.POINTER -> 4300L
            UnitType.NULL_POINTER -> 9000L
            UnitType.EXCEPTION_HANDLER -> 8000L
            UnitType.LOOP -> 5000L
            UnitType.RECURSIVE_CALL -> 7800L
            UnitType.MUTEX -> 7200L
            UnitType.SEMAPHORE -> 7000L
            UnitType.OBSERVER -> 5200L
        }
    }

    fun requiredFactoryFor(unitType: UnitType): FactoryType {
        return when (unitType) {
            UnitType.ALLOCATOR,
            UnitType.BUFFER,
            UnitType.MEMORY_POOL,
            UnitType.DMA_CONTROLLER,
            UnitType.CPU_SCHEDULER,
            UnitType.LOAD_BALANCER,
            UnitType.INTERRUPT_HANDLER,
            UnitType.INJECTOR,
            UnitType.CACHE_RUNNER,
            UnitType.COROUTINE_ARCHER,
            UnitType.STACK_FRAME,
            UnitType.HEAP_BLOCK,
            UnitType.POINTER,
            UnitType.LOOP,
            UnitType.RECURSIVE_CALL -> FactoryType.BASIC

            UnitType.GARBAGE_COLLECTOR,
            UnitType.THREAD_GUARD,
            UnitType.FIREWALL,
            UnitType.PATCH_HEALER,
            UnitType.EXCEPTION_HANDLER,
            UnitType.MUTEX,
            UnitType.SEMAPHORE,
            UnitType.OBSERVER,
            UnitType.DEADLOCK,
            UnitType.OVERCLOCK,
            UnitType.NULL_POINTER -> FactoryType.SUPPORT
        }
    }

    fun isManualTargetCard(unitType: UnitType): Boolean {
        return unitType == UnitType.DEADLOCK || unitType == UnitType.OVERCLOCK || unitType == UnitType.NULL_POINTER
    }

    fun memoryBatchFor(unitType: UnitType): Int {
        return when (unitType) {
            UnitType.ALLOCATOR -> 100
            UnitType.BUFFER -> 120
            UnitType.MEMORY_POOL -> 180
            UnitType.DMA_CONTROLLER -> 140
            else -> 100
        }
    }

    fun memoryWorkStartText(unitType: UnitType): String {
        return when (unitType) {
            UnitType.ALLOCATOR -> "allocating memory..."
            UnitType.BUFFER -> "buffering memory chunk..."
            UnitType.MEMORY_POOL -> "preallocating pool..."
            UnitType.DMA_CONTROLLER -> "DMA transfer into memory..."
            else -> "allocating memory..."
        }
    }

    // Константы для специфичной логики юнитов
    object SpecialConstants {
        const val DEFENSIVE_FIREWALL_SIDE_OFFSET = 0f
        const val DEFENSIVE_MUTEX_SIDE_OFFSET = -70f
        const val DEFENSIVE_SEMAPHORE_SIDE_OFFSET = 70f
        const val DEFENSIVE_EXCEPTION_HANDLER_OFFSET = 35f
        const val DEFENSIVE_HEAP_BLOCK_OFFSET = -35f
        const val DEFENSIVE_FORWARD_OFFSET = 150f

        const val DEADLOCK_STUN_DURATION = 2500L
        const val OVERCLOCK_DURATION = 4500L
        const val NULL_POINTER_STUN_DURATION = 1600L
        const val NULL_POINTER_DAMAGE = 18

        const val POINTER_MARK_RANGE = 160f
        const val OBSERVER_MARK_RANGE = 190f
        const val POINTER_MARK_DURATION = 2200L

        const val STACK_FRAME_LIFETIME = 1600L
        const val RECURSIVE_CALL_CHECK_INTERVAL = 2200L
        const val RECURSIVE_CALL_STACK_OVERFLOW_LIMIT = 5
        const val RECURSIVE_CALL_OVERFLOW_DAMAGE = 18
        const val LOOP_TICK_INTERVAL = 900L

        const val MARKED_DAMAGE_MULTIPLIER = 1.28f
        const val PROTECTED_DAMAGE_REDUCTION = 0.70f
        const val OVERCLOCK_COOLDOWN_REDUCTION = 0.7f

        const val GARBAGE_COLLECTOR_WORK_DURATION = 800L
        const val GARBAGE_COLLECTOR_WORK_RANGE = 70f

        const val MEMORY_WORK_DURATION = 2200L  // Время работы захватчика памяти до исчезновения

        const val PATCH_HEALER_COOLDOWN = 1200L
        const val PATCH_HEALER_HEALING = 14
        const val PATCH_HEALER_SEARCH_RANGE = 115f

        const val GC_TARGET_SEARCH_RANGE = 70f

        const val MOVEMENT_SPEED_OVERCLOCK_MULTIPLIER = 1.45f
        const val MOVEMENT_SPEED_THROUGHPUT_MULTIPLIER = 1.22f
    }
}

