package com.project.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.project.client.engine.MatchHandler
import com.project.client.network.api.ListenSocket
import com.project.client.ui.screens.GameScreen
import com.project.client.ui.screens.LoginScreen
import com.project.client.ui.screens.MatchMakingScreen
import com.project.shared.api.events.Event
import com.project.shared.api.events.GameOverEvent
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.events.MatchFoundEvent
import com.project.shared.api.events.SystemMessageEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyGame : Game() {
    private var playerId: Int? = null
    private var username: String = "Player"

    val matchHandler = MatchHandler()

    private lateinit var listenSocket: ListenSocket
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun create() {
        setScreen(LoginScreen(this))
    }

    fun setProfile(playerId: Int, username: String) {
        this.playerId = playerId
        this.username = username

        if (::listenSocket.isInitialized) {
            listenSocket.close()
        }

        listenSocket = ListenSocket(playerId)
        listenSocket.onEvent = { event -> onEvent(event) }

        scope.launch {
            listenSocket.makeHandshake()
        }
    }

    fun getPlayerId(): Int {
        return playerId ?: error("Player is not logged in")
    }

    fun getUsername(): String {
        return username
    }

    private fun onEvent(event: Event) {
        Gdx.app.postRunnable {
            when (event) {
                is MatchFoundEvent -> {
                    (screen as? MatchMakingScreen)?.startGame(event)
                }

                is GameStartEvent -> {
                    (screen as? GameScreen)?.startGame(event)
                }

                is GameStateSnapshotEvent -> {
                    (screen as? GameScreen)?.updateGameState(event)
                }

                is GameOverEvent -> {
                    (screen as? GameScreen)?.finishGame(event)
                }

                is SystemMessageEvent -> {
                    (screen as? GameScreen)?.showSystemMessage(event.message)
                }
            }
        }
    }

    override fun dispose() {
        if (::listenSocket.isInitialized) {
            listenSocket.close()
        }

        super.dispose()
    }
}
