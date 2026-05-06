package com.project.server.service

import com.project.server.models.GameRoomData
import com.project.server.models.PlayerSession
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

object MatchQueue {
    private val queue = ConcurrentLinkedQueue<PlayerSession>()
    private val mutex = Mutex()

    suspend fun addPlayer(player: PlayerSession) {
        mutex.withLock {
            if (queue.any { it.playerId == player.playerId }) {
                return
            }

            queue.add(player)
            tryMatchLocked()
        }
    }

    suspend fun removePlayer(playerId: Int) {
        mutex.withLock {
            queue.removeIf { it.playerId == playerId }
        }
    }

    private suspend fun tryMatchLocked() {
        while (queue.size >= 2) {
            val player1 = queue.poll() ?: break
            val player2 = queue.poll() ?: break

            if (player1.playerId == player2.playerId) {
                queue.add(player1)
                break
            }

            val roomData = GameRoomData(
                id = UUID.randomUUID().toString(),
                players = listOf(player1, player2)
            )

            GameManager.loadGame(roomData)
        }
    }
}
