package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.MainStage

class MainScreen(game: MyGame) : BaseScreen(game) {
    override val stage = MainStage(viewport, game)
}