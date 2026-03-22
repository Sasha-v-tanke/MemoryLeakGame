package com.project.client.ui.screens

import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.project.client.MyGame
import com.project.client.network.api.MatchMakingSocket
import com.project.client.ui.stages.MatchMakingStage

class MatchMakingScreen(game: MyGame) : BaseScreen(game) {

    private val socket = MatchMakingSocket("/create_match", game.getPlayerId())
    override val stage = MatchMakingStage(viewport, game)

    override fun show() {
        super.show()
        stage.startTimer()

        stage.cancelButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                socket.cancelMatch()
                game.setScreen(MainScreen(game))
                true
            } else false
        }

        socket.onFindMatchResponse = { resp ->
            if (!resp.success) {
                stage.stopTimer()
                game.setScreen(MainScreen(game))
            }
        }

        socket.onMatchFound = { found ->
            stage.stopTimer()
            game.matchHandler.setMatch(found)
            game.setScreen(GameScreen(game))
        }

        socket.onCancelMatchResponse = { cancelResp ->
            if (!cancelResp.success) println("Cancel failed")
        }

        socket.onError = { e ->
            stage.stopTimer()
            println("WebSocket error: ${e.message}")
            game.setScreen(MainScreen(game))
        }

        socket.findMatch()
    }

    override fun hide() {
        super.hide()
        stage.stopTimer()
    }
}