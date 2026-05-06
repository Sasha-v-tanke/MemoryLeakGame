package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.RegisterStage

class RegisterScreen(game: MyGame) : BaseScreen(game) {
    override val stage = RegisterStage(viewport, game)
}
