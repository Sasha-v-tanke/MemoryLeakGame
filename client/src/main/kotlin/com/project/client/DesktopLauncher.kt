package com.project.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode

fun main() {
    val config = Lwjgl3ApplicationConfiguration()
    config.setTitle("Game Client")
    config.setWindowedMode(800, 600)
    config.setHdpiMode(HdpiMode.Logical)
    Lwjgl3Application(MyGame(), config)
}