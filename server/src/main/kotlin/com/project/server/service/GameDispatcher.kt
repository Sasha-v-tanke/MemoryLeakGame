package com.project.server.service

import com.project.server.models.PlayerSession
import com.project.shared.api.game.GameStartEvent
import com.project.shared.api.matchmaking.MatchFoundEvent
import io.ktor.websocket.Frame
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GameDispatcher {
    private val json = Json { encodeDefaults = true }

    suspend fun sendToPlayer(player: PlayerSession, message: MatchFoundEvent) {
        player.socket.send(Frame.Text(json.encodeToString(message)))
    }

    suspend fun notifyPlayers(players: List<PlayerSession>) {
        val message = GameStartEvent()
        players.forEach { player ->
            player.socket.send(Frame.Text(json.encodeToString(message)))
        }
    }
}