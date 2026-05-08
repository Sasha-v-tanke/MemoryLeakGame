package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.List
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.Window
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.ui.theme.UiTheme

open class BaseStage(viewport: Viewport) : Stage(viewport) {
    protected val skin = Skin(Gdx.files.internal("uiskin/uiskin.json"))

    init {
        skin.getAtlas().textures.forEach { texture ->
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }

        installReadableFont()
    }

    open fun buildUI() {}

    open fun show() {}

    protected fun screenRoot(padding: Float = 24f): Table {
        return Table().apply {
            setFillParent(true)
            pad(padding)
        }
    }

    protected fun panel(
        tint: Color = Color(0.04f, 0.07f, 0.12f, 0.9f),
        padding: Float = 14f
    ): Table {
        return Table(skin).apply {
            UiTheme.stylePanel(this, skin, tint, padding)
        }
    }

    protected fun titleLabel(text: String, scale: Float = 1.35f): Label {
        return Label(text, skin, "title").apply { UiTheme.styleTitle(this, scale) }
    }

    protected fun subtitleLabel(text: String, scale: Float = 1f): Label {
        return Label(text, skin).apply { UiTheme.styleSubtitle(this, scale) }
    }

    protected fun mutedLabel(text: String, scale: Float = 1f): Label {
        return Label(text, skin, "small").apply { UiTheme.styleMuted(this, scale) }
    }

    override fun dispose() {
        super.dispose()
        skin.dispose()
    }

    private fun installReadableFont() {
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Arial.ttf"))
        val defaultFont = generator.generateFont(fontParameter(16)).apply {
            setUseIntegerPositions(true)
            data.markupEnabled = false
        }
        val smallFont = generator.generateFont(fontParameter(13)).apply {
            setUseIntegerPositions(true)
            data.markupEnabled = false
        }
        val titleFont = generator.generateFont(fontParameter(24)).apply {
            setUseIntegerPositions(true)
            data.markupEnabled = false
        }
        generator.dispose()

        skin.add("readable-font", defaultFont, BitmapFont::class.java)
        skin.add("small-font", smallFont, BitmapFont::class.java)
        skin.add("title-font", titleFont, BitmapFont::class.java)
        applyFontToSkin(defaultFont, smallFont, titleFont)
    }

    private fun fontParameter(fontSize: Int): FreeTypeFontGenerator.FreeTypeFontParameter {
        return FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = fontSize
            minFilter = Texture.TextureFilter.Linear
            magFilter = Texture.TextureFilter.Linear
            hinting = FreeTypeFontGenerator.Hinting.Full
            characters = buildString {
                append(FreeTypeFontGenerator.DEFAULT_CHARS)
                append("АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ")
                append("абвгдеёжзийклмнопрстуфхцчшщъыьэюя")
                append("·—–№")
            }
        }
    }

    private fun applyFontToSkin(font: BitmapFont, smallFont: BitmapFont, titleFont: BitmapFont) {
        skin.getAll(Label.LabelStyle::class.java).values().forEach { style ->
            style.font = font
        }
        skin.getAll(TextButton.TextButtonStyle::class.java).values().forEach { style ->
            style.font = font
        }
        skin.getAll(TextField.TextFieldStyle::class.java).values().forEach { style ->
            style.font = font
            style.messageFont = font
        }
        skin.getAll(CheckBox.CheckBoxStyle::class.java).values().forEach { style ->
            style.font = font
        }
        skin.getAll(SelectBox.SelectBoxStyle::class.java).values().forEach { style ->
            style.font = font
        }
        skin.getAll(List.ListStyle::class.java).values().forEach { style ->
            style.font = font
        }
        skin.getAll(Window.WindowStyle::class.java).values().forEach { style ->
            style.titleFont = font
        }

        skin.add("small", Label.LabelStyle(skin.get(Label.LabelStyle::class.java)).apply {
            this.font = smallFont
        })
        skin.add("title", Label.LabelStyle(skin.get(Label.LabelStyle::class.java)).apply {
            this.font = titleFont
        })
        skin.add("card", TextButton.TextButtonStyle(skin.get(TextButton.TextButtonStyle::class.java)).apply {
            this.font = smallFont
        })
    }
}
