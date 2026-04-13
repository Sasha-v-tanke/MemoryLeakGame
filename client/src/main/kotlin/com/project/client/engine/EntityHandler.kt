package com.project.client.engine


import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.project.shared.engine.EntityState
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.Transform
import kotlin.collections.remove

class EntityHandler(private val addActor: (Image) -> Unit) {
    private val entityViews = mutableMapOf<Long, Image>()
    private val entityStates = mutableMapOf<Long, EntityState>()
    private val actorToEntityId = mutableMapOf<Image, Long>()

    private val aliveIds = HashSet<Long>()

    fun updateEntity(state: EntityState, entityID: Long) {
        aliveIds += entityID
        val transform = state.components.filterIsInstance<Transform>().firstOrNull() ?: return
        val sprite = state.components.filterIsInstance<Sprite>().firstOrNull()
        val texturePath = "objects/${sprite?.textureId ?: "default.png"}"

        Gdx.app.postRunnable {
            val image = entityViews.getOrPut(state.id) {
                Image(Texture(Gdx.files.internal(texturePath))).also { addActor(it) }
            }

            actorToEntityId[image] = state.id

            val scale = sprite?.scale ?: 1f
            image.setScale(scale)
            image.setPosition(transform.x - image.width / 2 * scale, transform.y - image.height / 2 * scale)
        }
        entityStates[state.id] = state
    }

    fun postUpdate() {
        val toRemove = entityViews.keys - aliveIds
        toRemove.forEach { id ->
            if (entityStates.containsKey(id)) {
                actorToEntityId.remove(entityViews[id])
                entityViews.remove(id)?.remove()
                entityStates.remove(id)
            }
        }
    }

    fun getEntity(image: Image): EntityState? {
        val entityID = actorToEntityId[image] ?: return null
        return entityStates[entityID]
    }
}