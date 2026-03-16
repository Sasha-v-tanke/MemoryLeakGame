package com.project.client.ui.stages

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame

class SettingsStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    lateinit var backButton: TextButton

    override fun buildUI() {
        val table = Table()
        table.setFillParent(true)
        table.top().left()
        this.addActor(table)

        backButton = TextButton("Back", skin)
        table.add(backButton).pad(10f)
    }

    override fun show() {

    }
}