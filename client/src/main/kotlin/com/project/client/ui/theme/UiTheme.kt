package com.project.client.ui.theme

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton

object UiTheme {
    val background = Color(0.012f, 0.015f, 0.026f, 1f)

    private val textPrimary = Color(0.93f, 0.97f, 1f, 1f)
    private val textSecondary = Color(0.70f, 0.80f, 0.90f, 1f)
    private val textMuted = Color(0.62f, 0.70f, 0.80f, 1f)

    private val buttonPrimary = Color(0.17f, 0.37f, 0.88f, 1f)
    private val buttonSecondary = Color(0.17f, 0.21f, 0.30f, 1f)
    private val buttonDanger = Color(0.66f, 0.17f, 0.22f, 1f)

    val statusOk = Color(0.68f, 0.95f, 0.75f, 1f)
    val statusWarn = Color(1f, 0.84f, 0.46f, 1f)
    val statusError = Color(1f, 0.50f, 0.50f, 1f)
    val statusInfo = Color(0.72f, 0.91f, 1f, 1f)

    fun stylePanel(table: Table, skin: Skin, tint: Color = Color(0.04f, 0.07f, 0.12f, 0.9f), padding: Float = 14f) {
        table.background = skin.newDrawable("default-round", tint)
        table.pad(padding)
    }

    fun styleTitle(label: Label, scale: Float = 1.35f) {
        label.setFontScale(1f)
        label.color = textPrimary
    }

    fun styleSubtitle(label: Label, scale: Float = 1f) {
        label.setFontScale(1f)
        label.color = textSecondary
    }

    fun styleMuted(label: Label, scale: Float = 1f) {
        label.setFontScale(1f)
        label.color = textMuted
    }

    fun stylePrimaryButton(button: TextButton, compact: Boolean = false) {
        button.color = buttonPrimary
        button.label.setFontScale(1f)
    }

    fun styleSecondaryButton(button: TextButton, compact: Boolean = false) {
        button.color = buttonSecondary
        button.label.setFontScale(1f)
    }

    fun styleDangerButton(button: TextButton, compact: Boolean = false) {
        button.color = buttonDanger
        button.label.setFontScale(1f)
    }
}
