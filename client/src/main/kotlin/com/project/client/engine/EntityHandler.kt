package com.project.client.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.project.shared.engine.EntityState
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit

class EntityHandler(private val addActor: (Group) -> kotlin.Unit) {
    private val entityViews = mutableMapOf<Long, Group>()
    private val entityStates = mutableMapOf<Long, EntityState>()
    private val actorToEntityId = mutableMapOf<Group, Long>()
    private val aliveIds = HashSet<Long>()

    fun beginSnapshot() {
        aliveIds.clear()
    }

    fun updateEntity(state: EntityState) {
        aliveIds += state.id

        val transform = state.components.filterIsInstance<Transform>().firstOrNull() ?: return
        val sprite = state.components.filterIsInstance<Sprite>().firstOrNull()
        val health = state.components.filterIsInstance<Health>().firstOrNull()

        val texturePath = sprite?.textureId ?: "objects/default.png"

        Gdx.app.postRunnable {
            val group = entityViews.getOrPut(state.id) {
                createEntityGroup(texturePath).also { addActor(it) }
            }

            actorToEntityId[group] = state.id

            val image = group.children.firstOrNull { it is Image } as? Image

            if (image != null && image.name != texturePath) {
                val newImage = Image(TextureCache.get(texturePath))
                image.drawable = newImage.drawable
                image.name = texturePath
            }

            val baseSize = getBaseSize(state)
            val configScale = sprite?.scale ?: 1f

            resizeGroup(group, baseSize * configScale)

            group.setPosition(transform.x, transform.y)

            group.color = Color.WHITE
            group.color.a = if (health?.isDead == true) 0.35f else 1f
        }

        entityStates[state.id] = state
    }

    fun endSnapshot() {
        val toRemove = entityViews.keys - aliveIds

        toRemove.forEach { id ->
            val group = entityViews.remove(id)

            if (group != null) {
                actorToEntityId.remove(group)
                group.remove()
            }

            entityStates.remove(id)
        }
    }

    fun getEntity(group: Group): EntityState? {
        val entityId = actorToEntityId[group] ?: return null
        return entityStates[entityId]
    }

    fun getEntityById(id: Long): EntityState? {
        return entityStates[id]
    }

    private fun createEntityGroup(texturePath: String): Group {
        val image = Image(TextureCache.get(texturePath))
        image.name = texturePath

        return Group().apply {
            addActor(image)
            color = Color.WHITE
            setSize(image.width, image.height)
        }
    }

    private fun resizeGroup(group: Group, targetSize: Float) {
        val image = group.children.firstOrNull { it is Image } as? Image ?: return

        val textureWidth = image.prefWidth.takeIf { it > 0f } ?: image.width
        val textureHeight = image.prefHeight.takeIf { it > 0f } ?: image.height

        if (textureWidth <= 0f || textureHeight <= 0f) return

        val aspect = textureWidth / textureHeight

        val width: Float
        val height: Float

        if (aspect >= 1f) {
            width = targetSize
            height = targetSize / aspect
        } else {
            height = targetSize
            width = targetSize * aspect
        }

        image.setSize(width, height)
        image.setPosition(-width / 2f, -height / 2f)

        group.setSize(width, height)
        group.setOrigin(width / 2f, height / 2f)
    }

    private fun getBaseSize(state: EntityState): Float {
        return when {
            state.components.any { it is Core } -> 132f
            state.components.any { it is Factory } -> 112f
            state.components.any { it is ResourceNode } -> 96f
            state.components.any { it is Unit } -> 58f
            else -> 72f
        }
    }
}
