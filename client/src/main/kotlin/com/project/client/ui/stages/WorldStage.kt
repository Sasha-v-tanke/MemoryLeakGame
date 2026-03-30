package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.shared.engine.GameConfig
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.engine.gameobjects.components.Sprite
import com.project.shared.engine.gameobjects.components.Transform

class WorldStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {

    private lateinit var background: Image
    private val entityViews = mutableMapOf<Long, Image>()

    override fun buildUI() {
        val texture = Texture(Gdx.files.internal("background/back-game.png"))
        background = Image(texture)
        background.setSize(GameConfig.worldWidth, GameConfig.worldHeight)
        background.touchable = Touchable.disabled
        addActor(background)
    }

    fun applySnapshot(snapshot: GameStateSnapshotEvent) {
        val aliveIds = HashSet<Long>(snapshot.entities.size)

        snapshot.entities.forEach { state ->
            aliveIds += state.id

            val transform = state.components.filterIsInstance<Transform>().firstOrNull() ?: return@forEach
            val sprite = state.components.filterIsInstance<Sprite>().firstOrNull()
            val texturePath = "objects/${sprite?.textureId ?: "core.png"}"

            Gdx.app.postRunnable {
                val image = entityViews.getOrPut(state.id) {
                    Image(Texture(Gdx.files.internal(texturePath))).also { addActor(it) }
                }
                image.setPosition(transform.x - image.width / 2, transform.y - image.height / 2)
            }
        }

        val toRemove = entityViews.keys - aliveIds
        toRemove.forEach { id ->
            entityViews.remove(id)?.remove()
        }
    }
}