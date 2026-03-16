package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.LoginStage

class LoginScreen(game: MyGame) : BaseScreen(game) {
    override val stage = LoginStage(viewport, game)

}
