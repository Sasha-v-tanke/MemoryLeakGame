package com.project.shared.engine.entities.units

import kotlinx.serialization.Serializable

@Serializable
enum class UnitType {
    ALLOCATOR,
    GARBAGE_COLLECTOR,
    THREAD_GUARD,
    INJECTOR,
    DEADLOCK,
    OVERCLOCK,
    CACHE_RUNNER,
    FIREWALL,
    COROUTINE_ARCHER,
    PATCH_HEALER,
    STACK_FRAME,
    HEAP_BLOCK,
    POINTER,
    NULL_POINTER,
    EXCEPTION_HANDLER,
    LOOP,
    RECURSIVE_CALL,
    MUTEX,
    SEMAPHORE,
    OBSERVER,
    BUFFER,
    MEMORY_POOL,
    DMA_CONTROLLER,
    CPU_SCHEDULER,
    LOAD_BALANCER,
    INTERRUPT_HANDLER
}
