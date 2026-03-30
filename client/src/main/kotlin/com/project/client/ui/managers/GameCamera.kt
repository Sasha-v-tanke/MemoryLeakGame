package com.project.client.ui.managers

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.OrthographicCamera
import com.project.client.MyGame
import com.project.shared.engine.GameConfig

class GameCamera(private val game: MyGame) : OrthographicCamera() {

    fun initialize() {
        println("Index = ${game.matchHandler.getPlayerIndex()}")
        position.x = if (game.matchHandler.getPlayerIndex() == 1) viewportWidth / 2 else GameConfig.worldWidth - viewportWidth / 2
        position.y = if (game.matchHandler.getPlayerIndex() == 1) viewportHeight / 2 else GameConfig.worldHeight - viewportHeight / 2
        println("Position: ${position.x}, ${position.y}")
        update()
        println("Position: ${position.x}, ${position.y}")
    }

    fun update(delta: Float) {
        val speed = 300f * delta

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) or Gdx.input.isKeyPressed(Input.Keys.A)) position.x -= speed
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) or Gdx.input.isKeyPressed(Input.Keys.D)) position.x += speed
        if (Gdx.input.isKeyPressed(Input.Keys.UP) or Gdx.input.isKeyPressed(Input.Keys.W)) position.y += speed
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) or Gdx.input.isKeyPressed(Input.Keys.S)) position.y -= speed

        position.x = position.x.coerceIn(viewportWidth / 2, GameConfig.worldWidth - viewportWidth / 2)
        position.y = position.y.coerceIn(viewportHeight / 2, GameConfig.worldHeight - viewportHeight / 2)

        update()
    }
}