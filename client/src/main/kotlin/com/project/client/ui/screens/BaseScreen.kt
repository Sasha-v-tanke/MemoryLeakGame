package com.project.client.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.project.client.MyGame
import com.project.client.ui.stages.BaseStage
import com.project.client.ui.theme.UiTheme

abstract class BaseScreen(protected val game: MyGame) : ScreenAdapter() {
    protected val viewport = ExtendViewport(1280f, 720f)
    protected abstract val stage: BaseStage

    override fun show() {
        Gdx.input.inputProcessor = stage
        stage.clear()
        stage.buildUI()
        stage.show()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(UiTheme.background.r, UiTheme.background.g, UiTheme.background.b, UiTheme.background.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height, true)
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        stage.dispose()
    }
}
