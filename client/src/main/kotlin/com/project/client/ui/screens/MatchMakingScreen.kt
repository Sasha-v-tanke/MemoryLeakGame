package com.project.client.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.project.client.MyGame
import com.project.client.network.api.MatchMakingSocket
import com.project.client.ui.stages.MatchMakingStage
import com.project.shared.api.events.MatchFoundEvent

class MatchMakingScreen(game: MyGame) : BaseScreen(game) {
    private val socket = MatchMakingSocket(game.getPlayerId())

    override val stage = MatchMakingStage(viewport, game)

    override fun show() {
        super.show()

        stage.startTimer()

        stage.cancelButton.addListener { event ->
            if (event is InputEvent && event.type == InputEvent.Type.touchDown) {
                socket.cancelMatch { response ->
                    if (response.success) {
                        game.setScreen(MainScreen(game))
                    } else {
                        stage.setStatus("Cancel failed: ${response.description}")
                    }
                }
                true
            } else {
                false
            }
        }

        socket.onFindError = { e ->
            Gdx.app.postRunnable {
                stage.stopTimer()
                stage.setStatus("Search error: ${e.message}")
                game.setScreen(MainScreen(game))
            }
        }

        socket.findMatch { response ->
            if (!response.success) {
                stage.stopTimer()
                stage.setStatus(response.description)
                game.setScreen(MainScreen(game))
            }
        }
    }

    fun startGame(response: MatchFoundEvent) {
        stage.stopTimer()

        Gdx.app.postRunnable {
            game.matchHandler.setMatch(response)
            game.setScreen(GameScreen(game))
        }
    }

    override fun hide() {
        stage.stopTimer()
        socket.close()
        super.hide()
    }

    override fun dispose() {
        socket.close()
        super.dispose()
    }
}
