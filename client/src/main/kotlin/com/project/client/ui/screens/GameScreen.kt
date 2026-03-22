package com.project.client.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.project.client.MyGame
import com.project.client.ui.managers.GameCamera
import com.project.client.ui.stages.UIStage
import com.project.client.ui.stages.WorldStage

class GameScreen(private val game: MyGame) : ScreenAdapter() {
    private val worldViewport = ExtendViewport(800f, 600f)
    private val uiViewport = ScreenViewport()

    private val camera = GameCamera(game)

    private val worldStage = WorldStage(worldViewport, game)
    private val uiStage = UIStage(uiViewport, game)

    override fun show() {
        worldStage.buildUI()
        uiStage.buildUI()

        worldViewport.camera = camera
        camera.initialize(game)
        Gdx.input.inputProcessor = InputMultiplexer(uiStage, worldStage)

        worldStage.show()
        uiStage.show()
    }

    override fun render(delta: Float) {
        camera.update(delta)

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        worldStage.act(delta)
        worldStage.draw()

        uiStage.act(delta)
        uiStage.draw()
    }

    override fun resize(width: Int, height: Int) {
        worldViewport.update(width, height, true)
        uiViewport.update(width, height, true)
    }

    override fun dispose() {
        worldStage.dispose()
        uiStage.dispose()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }
}