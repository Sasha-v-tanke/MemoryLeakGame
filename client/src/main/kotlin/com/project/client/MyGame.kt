package com.project.client

import com.badlogic.gdx.Game
import com.project.client.engine.MatchHandler
import com.project.client.network.api.ListenSocket
import com.project.client.ui.screens.GameScreen
import com.project.client.ui.screens.LoginScreen
import com.project.client.ui.screens.MatchMakingScreen
import com.project.shared.api.events.Event
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.events.MatchFoundEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

        CoroutineScope(SupervisorJob()).launch {
            listenSocket.makeHandshake()
        }
    }

    fun getPlayerId(): Int {
        return playerID!!
    }

    fun onEvent(event: Event) {
        when (event) {
            is GameStartEvent -> (screen as? GameScreen)?.startGame()
            is MatchFoundEvent -> (screen as? MatchMakingScreen)?.startGame(event)
        }
    }
}