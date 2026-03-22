package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.engine.GameConfig
import com.project.client.ui.screens.MainScreen

class WorldStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {

    private lateinit var background: Image

    override fun buildUI() {
        val texture = Texture(Gdx.files.internal("back-game.png"))
        background = Image(texture)
        background.setSize(GameConfig.worldWidth, GameConfig.worldHeight)
        background.touchable = Touchable.disabled
        addActor(background)
    }
}