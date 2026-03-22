package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame

open class BaseStage(viewport: Viewport) : Stage(viewport) {

    protected val skin = Skin(Gdx.files.internal("uiskin.json"))
    protected val font = BitmapFont(Gdx.files.internal("default.fnt"))

    open fun buildUI() {}

    override fun dispose() {
        super.dispose()
        skin.dispose()
        font.dispose()
    }

    open fun show() {}
}