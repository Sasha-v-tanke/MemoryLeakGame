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
import com.project.client.ui.screens.PuzzleScreen
import com.project.client.ui.screens.SettingsScreen
import com.project.client.ui.screens.TutorialScreen
import com.project.client.ui.theme.UiTheme

class MainStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    override fun buildUI() {
        val root = screenRoot()
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(660f)

        val title = titleLabel("Memory Leak Arena").apply {
            setAlignment(Align.center)
        }

        val playerLabel = subtitleLabel("Владелец процесса: ${game.getUsername()} #${game.getPlayerId()}", 1.03f).apply {
            setAlignment(Align.center)
        }

        val deckStatus = Label("Колода готова: ${game.getSelectedDeck().size}/${MyGame.DECK_SIZE} карт", skin).apply {
            setAlignment(Align.center)
            color = UiTheme.statusInfo
        }

        val concept = subtitleLabel(
            "Захватывайте узлы инфраструктуры, защищайте ресурсы, разломайте ядро противника.",
            0.98f
        ).apply {
            setAlignment(Align.center)
            wrap = true
        }

        val playButton = TextButton("Матч 1 на 1", skin)
        val puzzleButton = TextButton("Лаборатория", skin)
        val tutorialButton = TextButton("Обучение", skin)
        val deckButton = TextButton("Колода", skin)
        val settingsButton = TextButton("Параметры", skin)
        UiTheme.stylePrimaryButton(playButton)
        UiTheme.styleSecondaryButton(puzzleButton)
        UiTheme.styleSecondaryButton(tutorialButton)
        UiTheme.styleSecondaryButton(deckButton)
        UiTheme.styleSecondaryButton(settingsButton)

        box.defaults().pad(8f)
        box.add(title).growX().row()
        box.add(playerLabel).growX().padTop(2f).row()
        box.add(deckStatus).growX().padBottom(8f).row()
        box.add(concept).width(600f).padBottom(18f).row()
        box.add(playButton).height(48f).growX().row()
        box.add(puzzleButton).height(42f).growX().row()
        box.add(tutorialButton).height(42f).growX().row()
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

        puzzleButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(PuzzleScreen(game))
                true
            } else {
                false
            }
        }

        tutorialButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                game.setScreen(TutorialScreen(game))
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
