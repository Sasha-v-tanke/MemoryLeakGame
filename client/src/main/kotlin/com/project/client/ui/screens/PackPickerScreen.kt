package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.PackPickerStage

class PackPickerScreen(game: MyGame) : BaseScreen(game) {
    override val stage = PackPickerStage(viewport, game)
}
