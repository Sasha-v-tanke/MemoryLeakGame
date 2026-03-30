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
                socket.cancelMatch { resp ->
                    if (!resp.success) println("Cancel failed")
                    else Gdx.app.postRunnable {
                        game.setScreen(MainScreen(game))
                    }
                }
                true
            } else false
        }

        socket.onFindError = { e ->
            println("WebSocket error: ${e.message}")
            Gdx.app.postRunnable {
                stage.stopTimer()
                game.setScreen(MainScreen(game))
            }
        }

        socket.onCancelError = { e ->
            println("WebSocket error: ${e.message}")
        }

        socket.findMatch { resp ->
            if (!resp.success) {
                Gdx.app.postRunnable {
                    stage.stopTimer()
                    game.setScreen(MainScreen(game))
                }
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
        super.hide()
        stage.stopTimer()
        socket.close()
    }

    override fun dispose() {
        socket.close()
        super.dispose()
    }
}