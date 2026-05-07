package com.project.client.ui.stages

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.Viewport
import com.project.client.MyGame
import com.project.client.ui.theme.UiTheme
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
        val root = screenRoot()
        root.center()
        addActor(root)

        val box = panel()
        root.add(box).width(620f)

        val title = titleLabel("Searching Opponent...", 1.24f).apply {
            setAlignment(Align.center)
        }

        statusLabel = subtitleLabel("Waiting for another system instance").apply {
            setAlignment(Align.center)
            wrap = true
            color = UiTheme.statusInfo
        }

        timerLabel = Label("00:00", skin).apply {
            setAlignment(Align.center)
            color = Color(0.79f, 0.93f, 1f, 1f)
        }

        cancelButton = TextButton("Cancel", skin)
        UiTheme.styleDangerButton(cancelButton)

        box.defaults().pad(8f)
        box.add(title).growX().row()
        box.add(statusLabel).width(520f).padBottom(8f).row()
        box.add(timerLabel).growX().padBottom(6f).row()
        box.add(cancelButton).height(42f).growX().padTop(20f).row()
    }

    fun setStatus(text: String) {
        statusLabel.setText(text)
        statusLabel.color = if (text.contains("error", ignoreCase = true) || text.contains("failed", ignoreCase = true)) {
            UiTheme.statusError
        } else {
            UiTheme.statusInfo
        }
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
