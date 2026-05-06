package com.project.client.ui.managers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.project.client.MyGame
import com.project.shared.engine.config.GameConfig
import kotlin.math.min

class GameCamera(private val game: MyGame) : OrthographicCamera() {
    private val minZoom = 0.65f
    private val maxZoom = 1.15f

    fun initialize() {
        viewportWidth = 1280f
        viewportHeight = 720f

        position.x = if (game.matchHandler.getPlayerIndex() == 1) {
            viewportWidth / 2f
        } else {
            GameConfig.worldWidth - viewportWidth / 2f
        }

        position.y = if (game.matchHandler.getPlayerIndex() == 1) {
            viewportHeight / 2f
        } else {
            GameConfig.worldHeight - viewportHeight / 2f
        }

        zoom = 1f
        clampToWorld()
        update()
    }

    fun update(delta: Float) {
        val speed = 520f * delta * zoom

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            position.x -= speed
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            position.x += speed
        }

        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            position.y += speed
        }

        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            position.y -= speed
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            zoom = (zoom + 0.1f).coerceAtMost(maxZoom)
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            zoom = (zoom - 0.1f).coerceAtLeast(minZoom)
        }

        clampToWorld()
        update()
    }

    private fun clampToWorld() {
        val halfW = viewportWidth * zoom / 2f
        val halfH = viewportHeight * zoom / 2f

        position.x = clampAxis(
            value = position.x,
            halfSize = halfW,
            worldSize = GameConfig.worldWidth
        )

        position.y = clampAxis(
            value = position.y,
            halfSize = halfH,
            worldSize = GameConfig.worldHeight
        )
    }

    private fun clampAxis(value: Float, halfSize: Float, worldSize: Float): Float {
        val minValue = halfSize
        val maxValue = worldSize - halfSize

        return if (maxValue < minValue) {
            worldSize / 2f
        } else {
            value.coerceIn(minValue, maxValue)
        }
    }
}
