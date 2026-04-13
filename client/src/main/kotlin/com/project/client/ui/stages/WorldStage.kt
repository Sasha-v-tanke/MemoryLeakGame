package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.engine.EntityHandler
import com.project.shared.engine.config.GameConfig
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.engine.EntityState


class WorldStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    private lateinit var background: Image
    private val entityHandler = EntityHandler { addActor(it) }

    override fun buildUI() {
        val texture = Texture(Gdx.files.internal("background/back-game.png"))
        background = Image(texture)
        background.setSize(GameConfig.worldWidth, GameConfig.worldHeight)
        background.touchable = Touchable.disabled
        addActor(background)
    }

    fun applySnapshot(snapshot: GameStateSnapshotEvent) {
        snapshot.entities.forEach { state ->
            entityHandler.updateEntity(state, state.id)
        }
        entityHandler.postUpdate()
    }

    fun getHoveredEntity(screenX: Int, screenY: Int): EntityState? {
        val v = Vector2(screenX.toFloat(), screenY.toFloat())
        viewport.unproject(v)

        val hit = hit(v.x, v.y, true) as? Image ?: return null
        return entityHandler.getEntity(hit)
    }
}