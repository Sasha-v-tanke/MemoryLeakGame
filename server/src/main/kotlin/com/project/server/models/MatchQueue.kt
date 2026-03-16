package com.project.server.models

import com.project.server.service.GameManager
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

object MatchQueue {
    private val queue = ConcurrentLinkedQueue<PlayerSession>()

    suspend fun addPlayer(player: PlayerSession) {
        queue.add(player)
        println("Added $player")
        tryMatch()
    }

    fun removePlayer(player: PlayerSession) {
        println("Removed $player")
        queue.remove(player)
    }

    private suspend fun tryMatch() {
        while (queue.size >= 2) {
            val player1 = queue.poll()!!
            val player2 = queue.poll()!!
            println("${player1.playerId} vs ${player2.playerId}")
            val room = GameRoom(UUID.randomUUID().toString(), listOf(player1, player2))
            GameManager.startGame(room)
        }
    }
}