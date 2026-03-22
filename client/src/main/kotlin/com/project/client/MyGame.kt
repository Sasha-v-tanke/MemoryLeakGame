package com.project.client

import com.badlogic.gdx.Game
import com.project.client.engine.MatchHandler
import com.project.client.ui.screens.LoginScreen
import com.project.shared.api.MatchFound

class MyGame : Game() {
    private var playerID: Int? = null
    val matchHandler = MatchHandler()

    override fun create() {
        setScreen(LoginScreen(this))
    }

    fun setPlayerId(playerID: Int) {
        this.playerID = playerID
    }

    fun getPlayerId(): Int {
        return playerID!!
    }
}