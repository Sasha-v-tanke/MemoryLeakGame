package com.project.server.service

import com.project.server.models.PlayerSession
import com.project.shared.api.events.Event
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.events.MatchFoundEvent
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

    suspend fun notifyPlayers(players: List<PlayerSession>) {
        val message = GameStartEvent()
        println(message)
        players.forEach { player -> sendToPlayer(player, message) }
    }
}