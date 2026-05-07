package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ProcessPhase {
    RUNNING,
    COMPLETED,
    DEAD,
    GARBAGE_COLLECTING
}

@Serializable
@SerialName("process_state")
data class ProcessState(
    var phase: ProcessPhase = ProcessPhase.RUNNING,
    var phaseStartedAt: Long = 0L,
    var completedAt: Long = 0L,
    var lastEvent: String = "",
    var internalCounter: Int = 0,
    var nextActionAt: Long = 0L
) : Component
