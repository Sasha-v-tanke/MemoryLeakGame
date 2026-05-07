package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.PuzzleStage

class PuzzleScreen(game: MyGame) : BaseScreen(game) {
    override val stage = PuzzleStage(viewport, game)
}
