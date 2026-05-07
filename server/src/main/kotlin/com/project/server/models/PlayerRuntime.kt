package com.project.server.models

import com.project.shared.engine.PlayerResources
import com.project.shared.engine.config.GameConfig

data class PlayerRuntime(
    val playerId: Int,
    val playerIndex: Int,
    var memory: Int = GameConfig.startingMemory,
    var cpu: Int = GameConfig.startingCpu,
    var memoryIncome: Int = GameConfig.baseMemoryIncome,
    var cpuIncome: Int = GameConfig.baseCpuIncome,
    var memoryAllocatedTotal: Int = 0,
    var memoryFreedTotal: Int = 0,
    var factoriesBuilt: Int = 0
) {
    fun toResources(): PlayerResources {
        return PlayerResources(
            memory = memory,
            cpu = cpu,
            memoryIncome = memoryIncome,
            cpuIncome = cpuIncome,
            memoryAllocatedTotal = memoryAllocatedTotal,
            memoryFreedTotal = memoryFreedTotal,
            factoriesBuilt = factoriesBuilt
        )
    }
}
