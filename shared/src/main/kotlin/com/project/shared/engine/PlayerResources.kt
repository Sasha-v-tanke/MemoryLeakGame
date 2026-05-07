package com.project.shared.engine

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResources(
    val memory: Int,
    val cpu: Int,
    val memoryIncome: Int,
    val cpuIncome: Int,
    val memoryAllocatedTotal: Int = 0,
    val memoryFreedTotal: Int = 0,
    val factoriesBuilt: Int = 0
)
