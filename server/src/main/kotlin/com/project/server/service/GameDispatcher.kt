package com.project.server.service

import com.project.server.models.PlayerSession
import com.project.shared.api.JsonFormats
import com.project.shared.api.events.Event
import com.project.shared.api.events.GameStartEvent
import io.ktor.websocket.Frame
import kotlinx.serialization.encodeToString

object GameDispatcher {
    private val json = JsonFormats.default

    suspend fun sendToPlayer(player: PlayerSession, message: Event) {
        player.socket.send(Frame.Text(json.encodeToString(message)))
    }

    suspend fun sendToAllPlayers(players: List<PlayerSession>, message: Event) {
        players.forEach { player ->
            sendToPlayer(player, message)
        }
    }

    suspend fun notifyPlayersGameStarted(players: List<PlayerSession>) {
        sendToAllPlayers(players, GameStartEvent())
    }
}
