package com.project.server.service

import com.project.server.models.PlayerSession
import com.project.shared.api.events.Event
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.events.GameStateSnapshotEvent
import io.ktor.websocket.Frame
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object GameDispatcher {
    private val json = Json {
        encodeDefaults = true
        classDiscriminator = "type"
    }

    suspend fun sendToPlayer(player: PlayerSession, message: Event) {
        player.socket.send(Frame.Text(json.encodeToString(message)))
    }

    suspend fun sendToAllPlayers(players: List<PlayerSession>, message: Event) {
        players.forEach { player -> sendToPlayer(player, message) }
    }

    suspend fun notifyPlayers(players: List<PlayerSession>) {
        sendToAllPlayers(players, GameStartEvent())
    }
}