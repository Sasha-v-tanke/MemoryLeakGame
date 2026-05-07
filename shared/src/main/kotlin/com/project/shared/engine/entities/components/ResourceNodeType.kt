package com.project.shared.engine.entities.components

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ResourceNodeType {
    CPU,
    MEMORY
}

@Serializable
@SerialName("resource_node")
data class ResourceNode(
    val nodeType: ResourceNodeType,
    var capturedBy: Int? = null,
    var captureProgressPlayer1: Float = 0.5f,
    var captureProgressPlayer2: Float = 0.5f,
    val captureRadius: Float = 105f,
    val incomePerSecond: Int = 1,
    val totalCpuShare: Int = 4
) : Component
