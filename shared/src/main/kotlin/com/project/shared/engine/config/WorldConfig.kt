package com.project.shared.engine.config

import com.project.shared.engine.entities.OwnerType

object WorldConfig {
    val core1 = EntityConfig(OwnerType.PLAYER_1, 0.1f, 0.1f, 0.5f, "core.png")
    val core2 = EntityConfig(OwnerType.PLAYER_2, 0.9f, 0.9f, 0.5f, "core.png")

    val cpu1 = EntityConfig(OwnerType.WORLD, 0.2f, 0.8f, 0.4f, "cpu.png")
    val cpu2 = EntityConfig(OwnerType.WORLD, 0.8f, 0.2f, 0.4f, "cpu.png")

    val ram1 = EntityConfig(OwnerType.WORLD, 0.4f, 0.55f, 0.4f, "ram.png")
    val ram2 = EntityConfig(OwnerType.WORLD, 0.55f, 0.4f, 0.4f, "ram.png")

    val objects = listOf(core1, core2, cpu1, cpu2, ram1, ram2)
}