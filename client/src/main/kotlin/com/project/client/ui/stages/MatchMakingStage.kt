package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import kotlin.concurrent.fixedRateTimer

class MatchMakingStage(
    viewport: Viewport,
    private val game: MyGame
) : BaseStage(viewport) {
    lateinit var cancelButton: TextButton

    private lateinit var statusLabel: Label
    private lateinit var timerLabel: Label

    private var startTime: Long = 0L
    private var timerTask: java.util.Timer? = null

    override fun buildUI() {
        val root = Table()
        root.setFillParent(true)
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(520f)

        val title = Label("Searching opponent...", skin).apply {
            setAlignment(Align.center)
            fontScaleX = 1.2f
            fontScaleY = 1.2f
        }

        statusLabel = Label("Waiting for another system instance", skin).apply {
            setAlignment(Align.center)
            wrap = true
        }

        timerLabel = Label("00:00", skin).apply {
            setAlignment(Align.center)
        }

        cancelButton = TextButton("Cancel", skin)

        box.defaults().pad(8f)
        box.add(title).growX().row()
        box.add(statusLabel).width(440f).padBottom(12f).row()
        box.add(timerLabel).growX().row()
        box.add(cancelButton).height(42f).growX().padTop(20f).row()
    }

    fun setStatus(text: String) {
        statusLabel.setText(text)
    }

    fun startTimer() {
        startTime = System.currentTimeMillis()
        timerTask?.cancel()

        timerTask = fixedRateTimer(period = 1000L) {
            val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
            val minutes = elapsed / 60
            val seconds = elapsed % 60

            Gdx.app.postRunnable {
                timerLabel.setText(String.format("%02d:%02d", minutes, seconds))
            }
        }
    }

    fun stopTimer() {
        timerTask?.cancel()
        timerTask = null
    }
}
