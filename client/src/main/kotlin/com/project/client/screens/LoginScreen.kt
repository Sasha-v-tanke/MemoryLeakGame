package com.project.client.screens

import com.project.client.MyGame
import com.project.client.stages.LoginStage

class LoginScreen(game: MyGame) : BaseScreen(game) {
    override val stage = LoginStage(viewport, game)

}
