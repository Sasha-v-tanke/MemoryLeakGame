package com.project.client.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.project.client.MyGame
import com.project.client.stages.BaseStage

abstract class BaseScreen(protected val game: MyGame) : ScreenAdapter() {
    protected val viewport = ExtendViewport(800f, 600f)
    protected abstract val stage: BaseStage

    override fun show() {
        Gdx.input.inputProcessor = stage
        stage.show()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
    }
}
