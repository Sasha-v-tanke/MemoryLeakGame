package com.project.client.ui.screens

import com.project.client.MyGame
import com.project.client.ui.stages.GameStage

class GameScreen(game: MyGame, val roomId: String, val opponentId: Int) : BaseScreen(game) {
    override val stage = GameStage(viewport, game)

    override fun show() {
        println("$opponentId, $roomId")
        super.show()
    }
}