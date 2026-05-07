package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.engine.EntityHandler
import com.project.client.engine.TextureCache
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.engine.EntityState
import com.project.shared.engine.WorldTextEvent
import com.project.shared.engine.config.GameConfig
import com.project.shared.engine.entities.OwnerType

class WorldStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    var onWorldClicked: (Float, Float) -> Unit = { _, _ -> }

    private lateinit var background: Image
    private lateinit var backgroundTexture: Texture
    private var ownsBackgroundTexture: Boolean = false
    private val entityLayer = Group()
    private val hpLayer = Group()
    private val textLayer = Group()
    private val shownTextEvents = mutableSetOf<Long>()
    private val entityHandler = EntityHandler(
        addEntityActor = { entityLayer.addActor(it) },
        addHpActor = { hpLayer.addActor(it) }
    )

    override fun buildUI() {
        val backgroundFile = Gdx.files.internal("background/back-game.png")
        ownsBackgroundTexture = backgroundFile.exists()
        backgroundTexture = if (ownsBackgroundTexture) {
            Texture(backgroundFile).apply { setFilter(TextureFilter.Linear, TextureFilter.Linear) }
        } else {
            TextureCache.battlefieldBackground(GameConfig.worldWidth.toInt(), GameConfig.worldHeight.toInt())
        }

        background = Image(backgroundTexture)
        background.setSize(GameConfig.worldWidth, GameConfig.worldHeight)
        background.touchable = Touchable.enabled
        background.color = Color.WHITE
        addActor(background)

        addWorldOverlay()
        addActor(entityLayer)
        addActor(hpLayer)
        addActor(textLayer)

        background.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val stageCoords = Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
                viewport.unproject(stageCoords)
                onWorldClicked(stageCoords.x, stageCoords.y)
            }
        })
    }

    fun applySnapshot(snapshot: GameStateSnapshotEvent) {
        entityHandler.beginSnapshot()
        snapshot.entities.forEach { state -> entityHandler.updateEntity(state) }
        entityHandler.endSnapshot()
    }

    fun applyTextEvents(events: List<WorldTextEvent>) {
        events.forEach { event ->
            if (event.id in shownTextEvents) return@forEach
            shownTextEvents += event.id

            val label = Label(event.text, skin, "small").apply {
                color = when (event.owner) {
                    OwnerType.PLAYER_1 -> Color(0.55f, 0.85f, 1f, 1f)
                    OwnerType.PLAYER_2 -> Color(1f, 0.55f, 0.55f, 1f)
                    OwnerType.WORLD -> Color.WHITE
                }
                setPosition(event.x, event.y)
                setFontScale(0.9f)
                touchable = Touchable.disabled
            }

            textLayer.addActor(label)
            label.addAction(
                Actions.sequence(
                    Actions.parallel(
                        Actions.moveBy(0f, 32f, event.ttlMillis / 1000f),
                        Actions.fadeOut(event.ttlMillis / 1000f)
                    ),
                    Actions.removeActor()
                )
            )
        }

        if (shownTextEvents.size > 200) {
            shownTextEvents.clear()
        }
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

    private fun addWorldOverlay() {
        val lineTexture = TextureCache.solidWhite()
        val gridSpacing = 120f
        var x = 0f
        while (x <= GameConfig.worldWidth) {
            val line = Image(lineTexture).apply {
                setSize(1f, GameConfig.worldHeight)
                setPosition(x, 0f)
                color = Color(0.42f, 0.58f, 0.76f, 0.10f)
                touchable = Touchable.disabled
            }
            addActor(line)
            x += gridSpacing
        }

        var y = 0f
        while (y <= GameConfig.worldHeight) {
            val line = Image(lineTexture).apply {
                setSize(GameConfig.worldWidth, 1f)
                setPosition(0f, y)
                color = Color(0.42f, 0.58f, 0.76f, 0.10f)
                touchable = Touchable.disabled
            }
            addActor(line)
            y += gridSpacing
        }

        addActor(Image(lineTexture).apply {
            setSize(GameConfig.worldWidth, 3f)
            setPosition(0f, GameConfig.worldHeight / 2f - 1.5f)
            color = Color(0.26f, 0.84f, 1f, 0.18f)
            touchable = Touchable.disabled
        })

        addActor(Image(lineTexture).apply {
            setSize(3f, GameConfig.worldHeight)
            setPosition(GameConfig.worldWidth / 2f - 1.5f, 0f)
            color = Color(0.26f, 0.84f, 1f, 0.14f)
            touchable = Touchable.disabled
        })
    }

    override fun dispose() {
        if (::backgroundTexture.isInitialized && ownsBackgroundTexture) backgroundTexture.dispose()
        super.dispose()
    }
}
