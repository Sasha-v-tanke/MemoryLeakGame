package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.SettingsStage

class SettingsScreen(game: MyGame) : BaseScreen(game) {
    override val stage = SettingsStage(viewport, game)
}