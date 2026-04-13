package com.project.client.ui.widgets

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.project.shared.engine.gameobjects.UnitConfig

class UnitCardButton(
    private val config: UnitConfig,
    skin: Skin
) : TextButton(buildButtonText(config), skin) {

    companion object {
        private fun buildButtonText(config: UnitConfig) = """
            ${config.unitType}
            CPU: ${config.costCPU} | RAM: ${config.costRAM}
            HP: ${config.health.toInt()} | Cap: ${config.capacity}
        """.trimIndent()
    }

    init {
//        pad(8f)
    }
}