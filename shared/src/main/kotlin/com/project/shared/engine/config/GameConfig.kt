package com.project.shared.engine.config

object GameConfig {
    const val worldWidth = 1600f
    const val worldHeight = 1200f

    const val tickMillis = 50L
    const val snapshotEveryTicks = 2L

    const val startingMemory = 14
    const val startingCpu = 8

    const val baseMemoryIncome = 2
    const val baseCpuIncome = 1

    const val resourceIncomeIntervalMillis = 1000L

    const val captureRadius = 105f
    const val coreAttackPriorityRange = 260f
}
