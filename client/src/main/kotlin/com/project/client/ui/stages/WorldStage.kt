package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.engine.EntityHandler
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.engine.EntityState
import com.project.shared.engine.config.GameConfig

class WorldStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    var onWorldClicked: (Float, Float) -> Unit = { _, _ -> }

    private lateinit var background: Image
    private val entityHandler = EntityHandler { addActor(it) }

    override fun buildUI() {
        val backgroundFile = Gdx.files.internal("background/back-game.png")
        val texture = if (backgroundFile.exists()) {
            Texture(backgroundFile)
        } else {
            Texture(Gdx.files.internal("objects/default.png"))
        }

        background = Image(texture)
        background.setSize(GameConfig.worldWidth, GameConfig.worldHeight)
        background.touchable = Touchable.enabled
        addActor(background)

        background.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val stageCoords = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                viewport.unproject(stageCoords)
                onWorldClicked(stageCoords.x, stageCoords.y)
            }
        })
    }

    fun applySnapshot(snapshot: GameStateSnapshotEvent) {
        println("[CLIENT][SNAPSHOT] tick=${snapshot.tick} entities=${snapshot.entities.size} resources=${snapshot.resources}")

        entityHandler.beginSnapshot()

        snapshot.entities.forEach { state ->
            entityHandler.updateEntity(state)
        }

        entityHandler.endSnapshot()
    }

    fun getHoveredEntity(screenX: Int, screenY: Int): EntityState? {
        val v = Vector2(screenX.toFloat(), screenY.toFloat())
        viewport.unproject(v)

        val hit = hit(v.x, v.y, true)

        val group = when (hit) {
            is Group -> hit
            else -> hit?.parent as? Group
        } ?: return null

        return entityHandler.getEntity(group)
    }
}
