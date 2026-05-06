package com.project.server.engine

object DebugLog {
    var enabled: Boolean = true

    var logTicks: Boolean = true
    var logSnapshots: Boolean = true
    var logIncome: Boolean = true
    var logCards: Boolean = true
    var logSpawns: Boolean = true
    var logMovement: Boolean = true
    var logCombat: Boolean = true
    var logCapture: Boolean = true

    private var lastTickLogAt: Long = 0L
    private var lastSnapshotLogAt: Long = 0L
    private var lastMovementLogAt: Long = 0L

    fun info(message: String) {
        if (!enabled) return
        println("[MLA] $message")
    }

    fun tick(message: String) {
        if (!enabled || !logTicks) return

        val now = System.currentTimeMillis()
        if (now - lastTickLogAt < 1000L) return

        lastTickLogAt = now
        println("[MLA][TICK] $message")
    }

    fun snapshot(message: String) {
        if (!enabled || !logSnapshots) return

        val now = System.currentTimeMillis()
        if (now - lastSnapshotLogAt < 1000L) return

        lastSnapshotLogAt = now
        println("[MLA][SNAPSHOT] $message")
    }

    fun income(message: String) {
        if (!enabled || !logIncome) return
        println("[MLA][INCOME] $message")
    }

    fun card(message: String) {
        if (!enabled || !logCards) return
        println("[MLA][CARD] $message")
    }

    fun spawn(message: String) {
        if (!enabled || !logSpawns) return
        println("[MLA][SPAWN] $message")
    }

    fun movement(message: String) {
        if (!enabled || !logMovement) return

        val now = System.currentTimeMillis()
        if (now - lastMovementLogAt < 500L) return

        lastMovementLogAt = now
        println("[MLA][MOVE] $message")
    }

    fun combat(message: String) {
        if (!enabled || !logCombat) return
        println("[MLA][COMBAT] $message")
    }

    fun capture(message: String) {
        if (!enabled || !logCapture) return
        println("[MLA][CAPTURE] $message")
    }
}
