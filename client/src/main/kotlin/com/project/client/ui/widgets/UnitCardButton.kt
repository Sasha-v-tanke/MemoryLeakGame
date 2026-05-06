package com.project.client.ui.widgets

import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.project.shared.engine.entities.units.UnitConfig

class UnitCardButton(
    private val config: UnitConfig,
    skin: Skin
) : TextButton(buildButtonText(config), skin) {
    companion object {
        private fun buildButtonText(config: UnitConfig): String {
            return """
                ${config.displayName}
                ${config.role}
                M:${config.costMemory} C:${config.costCpu}
                HP:${config.health} DMG:${config.damage}
            """.trimIndent()
        }
    }

    init {
        label.setFontScale(0.82f)
        label.setWrap(true)
        pad(5f)
    }
}
