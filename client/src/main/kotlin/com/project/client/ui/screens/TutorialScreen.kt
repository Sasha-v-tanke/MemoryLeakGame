package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.TutorialStage

class TutorialScreen(game: MyGame) : BaseScreen(game) {
    override val stage = TutorialStage(viewport, game)
}

