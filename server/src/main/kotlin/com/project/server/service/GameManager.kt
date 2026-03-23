package com.project.server.service

import com.project.server.engine.GameRoom
import com.project.server.models.GameRoomData
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.api.matchmaking.MatchFoundEvent
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val rooms = ConcurrentHashMap<String, GameRoom>()

    suspend fun loadGame(room: GameRoomData) {
        rooms[room.id] = GameRoom(room.id, room.players)
        room.players.forEach { player ->
            val opponent = room.players.first { it.playerId != player.playerId }
            val message = MatchFoundEvent(
                roomId = room.id,
                opponentId = opponent.playerId,
                index = if (player == room.players[0]) 1 else 2,
                type = "MatchFound"
            )
            GameDispatcher.sendToPlayer(player, message)
        }
    }

    suspend fun setPlayerReady(playerReady: PlayerReadyRequest) {
        val room = rooms[playerReady.roomId] ?: throw IllegalArgumentException("Room not found")
        room.setPlayerReady(playerReady)
    }
}