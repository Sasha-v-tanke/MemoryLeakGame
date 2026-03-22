package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.network.api.MatchMakingSocket
import kotlin.concurrent.fixedRateTimer

class MatchMakingStage(viewport: Viewport, private val game: MyGame) : BaseStage(viewport) {
    lateinit var cancelButton: TextButton
    lateinit var statusLabel: Label
    lateinit var timerLabel: Label
    private var startTime: Long = 0
    private var timerTask: java.util.Timer? = null

    override fun buildUI() {
        val table = Table()
        table.setFillParent(true)
        table.top().center()
        addActor(table)

        statusLabel = Label("Searching opponents…", skin)
        table.add(statusLabel).padTop(50f).row()

        timerLabel = Label("00:00", skin)
        table.add(timerLabel).padTop(20f).row()

        cancelButton = TextButton("Cancel", skin)
        table.add(cancelButton).padTop(40f)
    }

    fun startTimer() {
        startTime = System.currentTimeMillis()
        timerTask?.cancel()
        timerTask = fixedRateTimer(period = 1000) {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            val minutes = elapsed / 60
            val seconds = elapsed % 60
            Gdx.app.postRunnable { timerLabel.setText(String.format("%02d:%02d", minutes, seconds)) }
        }
    }

    fun stopTimer() {
        timerTask?.cancel()
        timerTask = null
    }
}