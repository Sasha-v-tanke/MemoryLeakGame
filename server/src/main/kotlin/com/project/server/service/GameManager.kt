package com.project.server.service

import com.project.server.models.GameRoom
import com.project.server.models.PlayerSession
import com.project.shared.api.MatchFound
import io.ktor.websocket.Frame
import kotlinx.coroutines.isActive
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GameManager {
    private val json = Json { encodeDefaults = true }

    suspend fun startGame(room: GameRoom) {
        room.players.forEach { player ->
            val opponent = room.players.first { it.playerId != player.playerId }
            val message = MatchFound(
                roomId = room.id,
                opponentId = opponent.playerId,
                type = "MatchFound"
            )
            println(message)
            sendToPlayer(player, message)
        }
    }

    private suspend fun sendToPlayer(player: PlayerSession, message: MatchFound) {
        println(player.socket)
        println(player.socket.isActive)
        player.socket.send(Frame.Text(json.encodeToString(message)))
    }
}