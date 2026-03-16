package com.project.client

import com.badlogic.gdx.Game
import com.project.client.screens.LoginScreen

class MyGame : Game() {

    override fun create() {
        setScreen(LoginScreen(this))
    }
}