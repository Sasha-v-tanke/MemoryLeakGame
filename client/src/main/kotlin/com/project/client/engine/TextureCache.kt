package com.project.client.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap

object TextureCache {
    private val textures = mutableMapOf<String, Texture>()
    private var solidWhiteTexture: Texture? = null
    private var softCircleTexture: Texture? = null
    private var battlefieldTexture: Texture? = null

    fun get(path: String): Texture {
        val normalizedPath = path

        return textures.getOrPut(normalizedPath) {
            val file = Gdx.files.internal(normalizedPath)
            if (file.exists()) {
                Texture(file, true).apply {
                    setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear)
                }
            } else {
                Gdx.app.error("TextureCache", "Texture file not found: $normalizedPath")
                Texture(Gdx.files.internal("objects/default.png"), true).apply {
                    setFilter(TextureFilter.MipMapLinearLinear, TextureFilter.Linear)
                }
            }
        }
    }

    fun solidWhite(): Texture {
        solidWhiteTexture?.let { return it }

        val pixmap = Pixmap(2, 2, Pixmap.Format.RGBA8888)
        pixmap.setColor(1f, 1f, 1f, 1f)
        pixmap.fill()

        val texture = Texture(pixmap).apply {
            setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
            setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
        }
        pixmap.dispose()
        solidWhiteTexture = texture
        return texture
    }

    fun softCircle(): Texture {
        softCircleTexture?.let { return it }

        val size = 128
        val center = size / 2f
        val maxR = center - 1f
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val dx = x - center
                val dy = y - center
                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                val alpha = (1f - (dist / maxR)).coerceIn(0f, 1f)
                if (alpha > 0f) {
                    pixmap.setColor(1f, 1f, 1f, alpha * alpha * 0.9f)
                    pixmap.drawPixel(x, y)
                }
            }
        }

        val texture = Texture(pixmap).apply {
            setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
        }
        pixmap.dispose()
        softCircleTexture = texture
        return texture
    }

    fun battlefieldBackground(width: Int, height: Int): Texture {
        battlefieldTexture?.let { return it }

        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)

        // Base dark tech gradient
        for (y in 0 until height) {
            val t = y.toFloat() / height.toFloat()
            val r = 0.035f + t * 0.030f
            val g = 0.055f + t * 0.060f
            val b = 0.090f + t * 0.110f
            pixmap.setColor(r, g, b, 1f)
            pixmap.drawLine(0, y, width, y)
        }

        // Circuit-like horizontal/vertical traces
        pixmap.setColor(0.22f, 0.40f, 0.62f, 0.23f)
        var x = 32
        while (x < width) {
            pixmap.drawLine(x, 0, x, height)
            x += 96
        }
        var y = 24
        while (y < height) {
            pixmap.drawLine(0, y, width, y)
            y += 88
        }

        // Brighter lanes for battlefield readability
        pixmap.setColor(0.35f, 0.62f, 0.86f, 0.16f)
        pixmap.fillRectangle(width / 2 - 120, 0, 240, height)
        pixmap.fillRectangle(0, height / 2 - 90, width, 180)

        // Corner and center accents
        pixmap.setColor(0.62f, 0.84f, 1f, 0.20f)
        pixmap.fillCircle(width / 2, height / 2, 90)
        pixmap.setColor(0.18f, 0.32f, 0.50f, 0.25f)
        pixmap.fillCircle(0, 0, 220)
        pixmap.fillCircle(width, 0, 220)
        pixmap.fillCircle(0, height, 220)
        pixmap.fillCircle(width, height, 220)

        val texture = Texture(pixmap).apply {
            setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
        }
        pixmap.dispose()
        battlefieldTexture = texture
        return texture
    }

    fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
        solidWhiteTexture?.dispose()
        softCircleTexture?.dispose()
        battlefieldTexture?.dispose()
        solidWhiteTexture = null
        softCircleTexture = null
        battlefieldTexture = null
    }
}
