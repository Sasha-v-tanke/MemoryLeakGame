package com.project.client.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.project.client.MyGame
import com.project.client.network.api.GameSocket
import com.project.client.ui.managers.GameCamera
import com.project.client.ui.stages.UIStage
import com.project.client.ui.stages.WorldStage
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.game.GameResponse
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.Transform

class GameScreen(private val game: MyGame) : ScreenAdapter() {
    private val worldViewport = ExtendViewport(800f, 600f)
    private val uiViewport = ScreenViewport()
    private val camera = GameCamera(game)
    private val worldStage = WorldStage(worldViewport, game)
    private val uiStage = UIStage(uiViewport, game)
    private val socket = GameSocket()
    private var gameStarted = false

    override fun show() {
        worldStage.buildUI()
        uiStage.buildUI()

        worldViewport.camera = camera
        camera.initialize()
        Gdx.input.inputProcessor = InputMultiplexer(uiStage, worldStage)

        worldStage.show()
        uiStage.show()
        socket.sendPlayerReady(game.getPlayerId(), game.matchHandler.getRoomId()) { response -> onResult(response) }
    }

    override fun render(delta: Float) {
        camera.update(delta)

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        worldStage.act(delta)
        worldStage.draw()

        uiStage.act(delta)
        uiStage.draw()

        val hovered = worldStage.getHoveredEntity(Gdx.input.x, Gdx.input.y)
        if (hovered != null) {
            val transform = hovered.components.filterIsInstance<Transform>().firstOrNull()
            val sprite = hovered.components.filterIsInstance<Sprite>().firstOrNull()

            uiStage.showHoverInfo(
                buildString {
                    appendLine("ID: ${hovered.id}")
                    appendLine("Owner: ${hovered.owner}")
                    if (transform != null) appendLine("Pos: %.1f, %.1f".format(transform.x, transform.y))
                    if (sprite != null) appendLine("Sprite: ${sprite.textureId} x${sprite.scale}")
                }
            )
        } else {
            uiStage.hideHoverInfo()
        }
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width, height, false)
        uiViewport.update(width, height, true)
    }

    override fun dispose() {
        socket.close()
        worldStage.dispose()
        uiStage.dispose()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
        socket.close()
    }

    fun startGame() {
        gameStarted = true
        uiStage.startGame()
    }

    fun onResult(response: GameResponse) {
        println("Game Result: $response")
    }

    fun updateGameState(snapshotEvent: GameStateSnapshotEvent) {
        worldStage.applySnapshot(snapshotEvent)
    }
}