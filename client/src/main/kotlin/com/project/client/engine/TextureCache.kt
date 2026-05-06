package com.project.client.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture

object TextureCache {
    private val textures = mutableMapOf<String, Texture>()

    fun get(path: String): Texture {
        val normalizedPath = path

        return textures.getOrPut(normalizedPath) {
            val file = Gdx.files.internal(normalizedPath)
            if (file.exists()) {
                Texture(file)
            } else {
                Texture(Gdx.files.internal("objects/default.png"))
            }
        }
    }

    fun dispose() {
        textures.values.forEach { it.dispose() }
        textures.clear()
    }
}
