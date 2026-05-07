package com.project.client.engine

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.project.shared.engine.EntityState
import com.project.shared.engine.entities.OwnerType
import com.project.shared.engine.entities.components.Core
import com.project.shared.engine.entities.components.Factory
import com.project.shared.engine.entities.components.Health
import com.project.shared.engine.entities.components.ResourceNode
import com.project.shared.engine.entities.components.ResourceNodeType
import com.project.shared.engine.entities.components.Sprite
import com.project.shared.engine.entities.components.Transform
import com.project.shared.engine.entities.components.Unit
import com.project.shared.engine.entities.units.UnitType

class EntityHandler(
    private val addEntityActor: (Group) -> kotlin.Unit,
    private val addHpActor: (Group) -> kotlin.Unit
) {
    companion object {
        private const val SHADOW_NAME = "shadow"
        private const val AURA_NAME = "aura"
        private const val SPRITE_NAME = "sprite"
        private const val HP_BG_NAME = "hp_bg"
        private const val HP_FILL_NAME = "hp_fill"
    }

    private val entityViews = mutableMapOf<Long, Group>()
    private val hpViews = mutableMapOf<Long, Group>()
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
            val entityGroup = entityViews.getOrPut(state.id) {
                createEntityGroup(texturePath).also { addEntityActor(it) }
            }
            val hpGroup = hpViews.getOrPut(state.id) {
                createHpGroup().also { addHpActor(it) }
            }

            actorToEntityId[entityGroup] = state.id

            val image = entityGroup.findActor<Image>(SPRITE_NAME)
            val currentTexturePath = image?.userObject as? String
            if (image != null && currentTexturePath != texturePath) {
                val newImage = Image(TextureCache.get(texturePath))
                image.drawable = newImage.drawable
                image.userObject = texturePath
            }

            val baseSize = getBaseSize(state)
            val configScale = sprite?.scale ?: 1f
            val targetSize = baseSize * configScale
            val (_, entityHeight) = resizeEntityGroup(entityGroup, targetSize)
            resizeHpGroup(hpGroup, targetSize)

            entityGroup.setPosition(transform.x, transform.y)
            hpGroup.setPosition(transform.x, transform.y + entityHeight / 2f + 9f)

            applyEntityVisuals(entityGroup, hpGroup, state, health)
        }

        entityStates[state.id] = state
    }

    fun endSnapshot() {
        val toRemove = entityViews.keys - aliveIds

        toRemove.forEach { id ->
            val entityGroup = entityViews.remove(id)
            val hpGroup = hpViews.remove(id)

            if (entityGroup != null) {
                actorToEntityId.remove(entityGroup)
                entityGroup.remove()
            }
            hpGroup?.remove()

            entityStates.remove(id)
        }
    }

    fun getEntity(group: Group): EntityState? {
        val entityId = actorToEntityId[group] ?: return null
        return entityStates[entityId]
    }

    private fun createEntityGroup(texturePath: String): Group {
        val shadow = Image(TextureCache.softCircle()).apply { name = SHADOW_NAME }
        val aura = Image(TextureCache.softCircle()).apply { name = AURA_NAME }
        val image = Image(TextureCache.get(texturePath)).apply {
            name = SPRITE_NAME
            userObject = texturePath
        }

        return Group().apply {
            addActor(shadow)
            addActor(aura)
            addActor(image)
            color = Color.WHITE
            setSize(image.width, image.height)
        }
    }

    private fun createHpGroup(): Group {
        val hpBg = Image(TextureCache.solidWhite()).apply { name = HP_BG_NAME }
        val hpFill = Image(TextureCache.solidWhite()).apply { name = HP_FILL_NAME }

        return Group().apply {
            addActor(hpBg)
            addActor(hpFill)
            touchable = Touchable.disabled
            color = Color.WHITE
        }
    }

    private fun resizeEntityGroup(group: Group, targetSize: Float): Pair<Float, Float> {
        val image = group.findActor<Image>(SPRITE_NAME) ?: return 0f to 0f
        val shadow = group.findActor<Image>(SHADOW_NAME)
        val aura = group.findActor<Image>(AURA_NAME)

        val textureWidth = image.prefWidth.takeIf { it > 0f } ?: image.width
        val textureHeight = image.prefHeight.takeIf { it > 0f } ?: image.height
        if (textureWidth <= 0f || textureHeight <= 0f) return 0f to 0f

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

        shadow?.apply {
            val shadowW = width * 0.92f
            val shadowH = height * 0.52f
            setSize(shadowW, shadowH)
            setPosition(-shadowW / 2f, -height / 2f - shadowH * 0.22f)
        }

        aura?.apply {
            val auraSize = targetSize * 0.90f
            setSize(auraSize, auraSize)
            setPosition(-auraSize / 2f, -auraSize / 2f)
        }

        group.setSize(width, height)
        group.setOrigin(width / 2f, height / 2f)
        return width to height
    }

    private fun resizeHpGroup(hpGroup: Group, targetSize: Float) {
        val hpBg = hpGroup.findActor<Image>(HP_BG_NAME)
        val hpFill = hpGroup.findActor<Image>(HP_FILL_NAME)

        val hpWidth = targetSize * 0.82f
        hpBg?.apply {
            setSize(hpWidth, 5f)
            setPosition(-hpWidth / 2f, -2.5f)
        }
        hpFill?.apply {
            setSize(hpWidth, 3f)
            setPosition(-hpWidth / 2f, -1.5f)
        }
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

    private fun applyEntityVisuals(
        entityGroup: Group,
        hpGroup: Group,
        state: EntityState,
        health: Health?
    ) {
        val unit = state.components.filterIsInstance<Unit>().firstOrNull()
        val isNode = state.components.any { it is ResourceNode }

        val sprite = entityGroup.findActor<Image>(SPRITE_NAME)
        val shadow = entityGroup.findActor<Image>(SHADOW_NAME)
        val aura = entityGroup.findActor<Image>(AURA_NAME)
        val hpBg = hpGroup.findActor<Image>(HP_BG_NAME)
        val hpFill = hpGroup.findActor<Image>(HP_FILL_NAME)

        val hpRatio = when {
            health == null || health.max <= 0 -> 1f
            else -> (health.current.toFloat() / health.max.toFloat()).coerceIn(0f, 1f)
        }

        if (isNode) {
            hpBg?.isVisible = false
            hpFill?.isVisible = false
        } else {
            hpBg?.isVisible = true
            hpFill?.isVisible = true
            hpBg?.color = Color(0f, 0f, 0f, 0.62f)
            hpFill?.color = when {
                hpRatio < 0.35f -> Color(1f, 0.37f, 0.37f, 0.96f)
                hpRatio < 0.7f -> Color(1f, 0.82f, 0.30f, 0.96f)
                else -> Color(0.44f, 0.95f, 0.59f, 0.96f)
            }
            hpFill?.setWidth((hpBg?.width ?: 0f) * hpRatio)
        }

        shadow?.color = Color(0f, 0f, 0f, 0.28f)
        aura?.color = auraColor(state)
        sprite?.color = spriteTint(state.owner, unit?.typeName)

        val deadAlpha = if (health?.isDead == true) 0.34f else 1f
        entityGroup.color = Color.WHITE
        entityGroup.color.a = deadAlpha
        hpGroup.color = Color.WHITE
        hpGroup.color.a = deadAlpha
    }

    private fun auraColor(state: EntityState): Color {
        val resourceNode = state.components.filterIsInstance<ResourceNode>().firstOrNull()
        if (resourceNode != null) {
            return when (resourceNode.nodeType) {
                ResourceNodeType.MEMORY -> Color(0.32f, 0.64f, 1f, 0.28f)
                ResourceNodeType.CPU -> Color(1f, 0.78f, 0.32f, 0.28f)
            }
        }

        return when (state.owner) {
            OwnerType.PLAYER_1 -> Color(0.25f, 0.63f, 1f, 0.22f)
            OwnerType.PLAYER_2 -> Color(1f, 0.38f, 0.38f, 0.22f)
            OwnerType.WORLD -> Color(0.76f, 0.76f, 0.76f, 0.18f)
        }
    }

    private fun spriteTint(owner: OwnerType, unitType: UnitType?): Color {
        if (unitType == null) {
            return when (owner) {
                OwnerType.PLAYER_1 -> Color(0.88f, 0.94f, 1f, 1f)
                OwnerType.PLAYER_2 -> Color(1f, 0.90f, 0.90f, 1f)
                OwnerType.WORLD -> Color(0.95f, 0.95f, 0.95f, 1f)
            }
        }

        return when (unitType) {
            UnitType.ALLOCATOR -> Color(0.70f, 0.90f, 1f, 1f)
            UnitType.CACHE_RUNNER -> Color(0.62f, 0.86f, 1f, 1f)
            UnitType.INJECTOR -> Color(1f, 0.72f, 0.72f, 1f)
            UnitType.COROUTINE_ARCHER -> Color(1f, 0.80f, 0.64f, 1f)
            UnitType.GARBAGE_COLLECTOR -> Color(0.74f, 1f, 0.80f, 1f)
            UnitType.PATCH_HEALER -> Color(0.80f, 1f, 0.88f, 1f)
            UnitType.THREAD_GUARD -> Color(0.90f, 0.84f, 1f, 1f)
            UnitType.FIREWALL -> Color(1f, 0.80f, 0.62f, 1f)
            UnitType.DEADLOCK -> Color(0.86f, 0.74f, 1f, 1f)
            UnitType.OVERCLOCK -> Color(1f, 0.90f, 0.68f, 1f)
        }
    }
}
