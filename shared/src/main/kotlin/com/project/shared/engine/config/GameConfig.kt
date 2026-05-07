package com.project.shared.engine.config

object GameConfig {
    const val worldWidth = 1600f
    const val worldHeight = 1200f

    const val tickMillis = 50L
    const val snapshotEveryTicks = 2L

    const val startingMemory = 10
    const val startingCpu = 10

    const val baseMemoryIncome = 0
    const val baseCpuIncome = 1
    const val basicFactoryMemoryIncomeBonus = 0
    const val supportFactoryCpuIncomeBonus = 1

    const val resourceIncomeIntervalMillis = 1000L

    const val captureRadius = 105f
    const val coreAttackPriorityRange = 260f

    const val memoryWorkRequired = 100f
    const val allocatorMemoryBatch = 8
    const val bufferMemoryBatch = 12
    const val memoryPoolBatch = 18
    const val dmaMemoryBatch = 10
    const val allocatorCpuCaptureWorkMillis = 2500L
    const val garbageCollectorWorkMillis = 1800L

    const val basicFactoryBuildMemoryCost = 14
    const val basicFactoryBuildCpuCost = 8
    const val supportFactoryBuildMemoryCost = 16
    const val supportFactoryBuildCpuCost = 10
    const val factoryBuildHealth = 360
    const val factoryProductionMultiplierBonus = 0.12f
}
