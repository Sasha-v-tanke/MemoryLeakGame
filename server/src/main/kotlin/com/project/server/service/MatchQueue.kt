package com.project.server.service

import com.project.server.models.GameRoomData
import com.project.server.models.PlayerSession
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

object MatchQueue {
    private val queue = ConcurrentLinkedQueue<PlayerSession>()

    suspend fun addPlayer(player: PlayerSession) {
        queue.add(player)
        tryMatch()
    }

    fun removePlayer(player: PlayerSession) {
        queue.remove(player)
    }

    private suspend fun tryMatch() {
        while (queue.size >= 2) {
            val player1 = queue.poll() ?: break
            val player2 = queue.poll() ?: break

            val roomData = GameRoomData(
                id = UUID.randomUUID().toString(),
                players = listOf(player1, player2)
            )

            GameManager.loadGame(roomData)
        }
    }
}
