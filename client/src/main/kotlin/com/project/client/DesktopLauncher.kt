package com.project.client

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.badlogic.gdx.graphics.glutils.HdpiMode

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Memory Leak Arena")
        setWindowedMode(1280, 720)
        setHdpiMode(HdpiMode.Logical)
        useVsync(true)
        setForegroundFPS(60)
        setIdleFPS(30)
    }

    Lwjgl3Application(MyGame(), config)
}
