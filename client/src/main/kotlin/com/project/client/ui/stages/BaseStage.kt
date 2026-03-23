package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.utils.viewport.Viewport

open class BaseStage(viewport: Viewport) : Stage(viewport) {

    protected val skin = Skin(Gdx.files.internal("uiskin/uiskin.json"))
    protected val font = BitmapFont(Gdx.files.internal("uiskin/default.fnt"))

    open fun buildUI() {}

    override fun dispose() {
        super.dispose()
        skin.dispose()
        font.dispose()
    }

    open fun show() {}
}