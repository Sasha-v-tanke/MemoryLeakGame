package com.project.client.screens

import com.project.client.MyGame
import com.project.client.stages.RegisterStage

class RegisterScreen(game: MyGame) : BaseScreen(game) {
    override val stage = RegisterStage(viewport, game)
}