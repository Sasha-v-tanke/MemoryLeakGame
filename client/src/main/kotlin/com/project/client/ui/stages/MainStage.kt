package com.project.client.ui.stages

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.screens.MatchMakingScreen
import com.project.client.ui.screens.PackPickerScreen
import com.project.client.ui.screens.SettingsScreen

class MainStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    override fun buildUI() {
        val root = Table()
        root.setFillParent(true)
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(560f)

        val title = Label("Memory Leak Arena", skin).apply {
            setAlignment(Align.center)
            fontScaleX = 1.35f
            fontScaleY = 1.35f
        }

        val playerLabel = Label("Instance owner: ${game.getUsername()} #${game.getPlayerId()}", skin).apply {
            setAlignment(Align.center)
        }

        val concept = Label(
            "Control Memory and CPU nodes, deploy cards from your deck, protect your Core and break the enemy system.",
            skin
        ).apply {
            setAlignment(Align.center)
            wrap = true
        }

        val playButton = TextButton("Find 1v1 Match", skin)
        val deckButton = TextButton("Deck / Cards", skin)
        val settingsButton = TextButton("Settings", skin)

        box.defaults().pad(8f)
        box.add(title).growX().row()
        box.add(playerLabel).growX().padBottom(14f).row()
        box.add(concept).width(500f).padBottom(18f).row()
        box.add(playButton).height(48f).growX().row()
        box.add(deckButton).height(42f).growX().row()
        box.add(settingsButton).height(42f).growX().row()

        playButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(MatchMakingScreen(game))
                true
            } else {
                false
            }
        }

        deckButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(PackPickerScreen(game))
                true
            } else {
                false
            }
        }

        settingsButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(SettingsScreen(game))
                true
            } else {
                false
            }
        }
    }
}
