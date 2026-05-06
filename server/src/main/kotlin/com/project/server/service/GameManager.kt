package com.project.server.service

import com.project.server.engine.GameRoom
import com.project.server.models.GameRoomData
import com.project.shared.api.events.MatchFoundEvent
import com.project.shared.api.game.PlayCardRequest
import com.project.shared.api.game.PlayCardResponse
import com.project.shared.api.game.PlayerReadyRequest
import java.util.concurrent.ConcurrentHashMap

object GameManager {
    private val rooms = ConcurrentHashMap<String, GameRoom>()

    suspend fun loadGame(room: GameRoomData) {
        val gameRoom = GameRoom(
            roomId = room.id,
            players = room.players,
            onRoomFinished = { finishedRoomId ->
                rooms.remove(finishedRoomId)
            }
        )

        rooms[room.id] = gameRoom

        room.players.forEachIndexed { index, player ->
            val opponent = room.players.first { it.playerId != player.playerId }

            val message = MatchFoundEvent(
                roomId = room.id,
                opponentId = opponent.playerId,
                index = index + 1,
                description = "Opponent found"
            )

            GameDispatcher.sendToPlayer(player, message)
        }
    }

    fun setPlayerReady(playerReady: PlayerReadyRequest) {
        val room = rooms[playerReady.roomId]
            ?: throw IllegalArgumentException("Room not found")

        room.setPlayerReady(playerReady)
    }

    fun playCard(request: PlayCardRequest): PlayCardResponse {
        val room = rooms[request.roomId]
            ?: return PlayCardResponse(false, "Room not found")

        return room.playCard(request)
    }
}
