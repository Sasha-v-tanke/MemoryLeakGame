package com.project.client

import com.badlogic.gdx.Game
import com.project.client.ui.screens.LoginScreen

class MyGame : Game() {
    private var playerID: Int? = null

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