package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.viewport.Viewport

open class BaseStage(viewport: Viewport) : Stage(viewport) {
    protected val skin = Skin(Gdx.files.internal("uiskin/uiskin.json"))
    protected val font = BitmapFont(Gdx.files.internal("uiskin/default.fnt"))

    open fun buildUI() {}

    open fun show() {}

    protected fun panel(): Table {
        return Table(skin).apply {
            background = skin.newDrawable("default-round", Color(0.04f, 0.06f, 0.10f, 0.88f))
            pad(12f)
        }
    }

    override fun dispose() {
        super.dispose()
        skin.dispose()
        font.dispose()
    }
}
