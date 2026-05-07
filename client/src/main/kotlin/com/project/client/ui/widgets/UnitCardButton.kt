package com.project.client.ui.widgets

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.project.shared.engine.entities.units.UnitConfig
import com.project.shared.engine.entities.units.UnitRole

class UnitCardButton(
    private val config: UnitConfig,
    skin: Skin
) : TextButton("", skin, "card") {
    private var cooldownMs: Long = 0L
    private var queueSize: Int = 0
    private var selected = false

    companion object {
        private fun buildButtonText(config: UnitConfig, cooldownMs: Long, queueSize: Int): String {
            val status = if (cooldownMs > 0L) {
                "Cooldown ${formatCooldown(cooldownMs)} · Queue $queueSize"
            } else {
                "Ready · Queue $queueSize"
            }

            return """
                ${config.displayName}
                ${formatRole(config.role)} · ${factoryTag(config.role)}
                Cost ${config.costMemory}M / ${config.costCpu}C
                Mem held ${config.allocatedMemory}
                $status
            """.trimIndent()
        }

        private fun formatCooldown(cooldownMs: Long): String {
            val tenths = (cooldownMs + 99L) / 100L
            return "${tenths / 10}.${tenths % 10}s"
        }

        private fun factoryTag(role: UnitRole): String {
            return when (role) {
                UnitRole.SPELL -> "Instant"
                UnitRole.CAPTURE, UnitRole.ATTACK -> "Basic Factory"
                UnitRole.SUPPORT, UnitRole.DEFENSE -> "Support Factory"
            }
        }

        private fun formatRole(role: UnitRole): String {
            return when (role) {
                UnitRole.CAPTURE -> "Resource"
                UnitRole.SUPPORT -> "Support"
                UnitRole.DEFENSE -> "Defense"
                UnitRole.ATTACK -> "Attack"
                UnitRole.SPELL -> "Spell"
            }
        }
    }

    init {
        refreshText()
        label.setFontScale(1f)
        label.setWrap(true)
        pad(6f)
        color = roleColor(config.role)
    }

    fun updateRuntime(cooldownMs: Long, queueSize: Int) {
        this.cooldownMs = cooldownMs
        this.queueSize = queueSize
        refreshText()
    }

    fun setSelected(isSelected: Boolean) {
        selected = isSelected
        color = if (selected) Color(0.29f, 0.52f, 0.95f, 1f) else roleColor(config.role)
    }

    private fun refreshText() {
        setText(buildButtonText(config, cooldownMs, queueSize))
    }

    private fun roleColor(role: UnitRole): Color {
        return when (role) {
            UnitRole.CAPTURE -> Color(0.17f, 0.31f, 0.54f, 1f)
            UnitRole.ATTACK -> Color(0.55f, 0.24f, 0.24f, 1f)
            UnitRole.DEFENSE -> Color(0.36f, 0.30f, 0.56f, 1f)
            UnitRole.SUPPORT -> Color(0.19f, 0.43f, 0.37f, 1f)
            UnitRole.SPELL -> Color(0.52f, 0.30f, 0.64f, 1f)
        }
    }
}
