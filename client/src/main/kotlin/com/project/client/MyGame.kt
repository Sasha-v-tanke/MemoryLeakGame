package com.project.client

import com.badlogic.gdx.Game
import com.project.client.engine.MatchHandler
import com.project.client.network.api.ListenSocket
import com.project.client.ui.screens.GameScreen
import com.project.client.ui.screens.LoginScreen
import com.project.shared.api.Event
import com.project.shared.api.game.GameStartEvent

class MyGame : Game() {
    private var playerID: Int? = null
    val matchHandler = MatchHandler()

    private lateinit var listenSocket: ListenSocket

    override fun create() {
        setScreen(LoginScreen(this))
    }

    fun setPlayerId(playerID: Int) {
        this.playerID = playerID

        listenSocket = ListenSocket(playerID)
        listenSocket.onEvent = { event -> onEvent(event) }
        listenSocket.connect()
    }

    fun getPlayerId(): Int {
        return playerID!!
    }

    fun onEvent(event: Event) {
        when (event::class.java) {
            GameStartEvent::class.java -> (screen as? GameScreen)?.startGame()
        }
//                text.contains("FindMatchResponse") -> {
//                    val resp = Json.decodeFromString<FindMatchResponse>(text)
//                    Gdx.app.postRunnable { onFindMatchResponse?.invoke(resp) }
//                }
//
//                text.contains("CancelMatchResponse") -> {
//                    val resp = Json.decodeFromString<CancelMatchResponse>(text)
//                    Gdx.app.postRunnable { onCancelMatchResponse?.invoke(resp) }
//                }
//
//                text.contains("MatchFound") -> {
//                    val found = Json.decodeFromString<MatchFoundEvent>(text)
//                    Gdx.app.postRunnable { onMatchFound?.invoke(found) }
//                }
    }
}