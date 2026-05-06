package com.project.shared.engine.config

import com.project.shared.engine.entities.OwnerType

object WorldConfig {
    val player1Core = EntityConfig(
        kind = WorldObjectKind.CORE,
        owner = OwnerType.PLAYER_1,
        x = 0.11f,
        y = 0.12f,
        scale = 1.25f,
        sprite = "core.png",
        health = 900
    )

    val player2Core = EntityConfig(
        kind = WorldObjectKind.CORE,
        owner = OwnerType.PLAYER_2,
        x = 0.89f,
        y = 0.88f,
        scale = 1.25f,
        sprite = "core.png",
        health = 900
    )

    val player1BasicFactory = EntityConfig(
        kind = WorldObjectKind.BASIC_FACTORY,
        owner = OwnerType.PLAYER_1,
        x = 0.20f,
        y = 0.13f,
        scale = 1.05f,
        sprite = "factory_basic.png",
        health = 500
    )

    val player2BasicFactory = EntityConfig(
        kind = WorldObjectKind.BASIC_FACTORY,
        owner = OwnerType.PLAYER_2,
        x = 0.80f,
        y = 0.87f,
        scale = 1.05f,
        sprite = "factory_basic.png",
        health = 500
    )

    val player1SupportFactory = EntityConfig(
        kind = WorldObjectKind.SUPPORT_FACTORY,
        owner = OwnerType.PLAYER_1,
        x = 0.12f,
        y = 0.22f,
        scale = 1.0f,
        sprite = "factory_support.png",
        health = 430
    )

    val player2SupportFactory = EntityConfig(
        kind = WorldObjectKind.SUPPORT_FACTORY,
        owner = OwnerType.PLAYER_2,
        x = 0.88f,
        y = 0.78f,
        scale = 1.0f,
        sprite = "factory_support.png",
        health = 430
    )

    val cpuTopLeft = EntityConfig(
        kind = WorldObjectKind.CPU_NODE,
        owner = OwnerType.WORLD,
        x = 0.26f,
        y = 0.77f,
        scale = 1.0f,
        sprite = "cpu.png"
    )

    val cpuBottomRight = EntityConfig(
        kind = WorldObjectKind.CPU_NODE,
        owner = OwnerType.WORLD,
        x = 0.74f,
        y = 0.23f,
        scale = 1.0f,
        sprite = "cpu.png"
    )

    val memoryCenterLeft = EntityConfig(
        kind = WorldObjectKind.MEMORY_NODE,
        owner = OwnerType.WORLD,
        x = 0.40f,
        y = 0.55f,
        scale = 1.0f,
        sprite = "memory.png"
    )

    val memoryCenterRight = EntityConfig(
        kind = WorldObjectKind.MEMORY_NODE,
        owner = OwnerType.WORLD,
        x = 0.60f,
        y = 0.45f,
        scale = 1.0f,
        sprite = "memory.png"
    )

    val memoryCenter = EntityConfig(
        kind = WorldObjectKind.MEMORY_NODE,
        owner = OwnerType.WORLD,
        x = 0.50f,
        y = 0.50f,
        scale = 1.12f,
        sprite = "memory.png"
    )

    val objects = listOf(
        player1Core,
        player2Core,
        player1BasicFactory,
        player2BasicFactory,
        player1SupportFactory,
        player2SupportFactory,
        cpuTopLeft,
        cpuBottomRight,
        memoryCenterLeft,
        memoryCenterRight,
        memoryCenter
    )
}
