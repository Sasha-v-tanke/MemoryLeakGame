package com.project.server.engine

import com.project.shared.api.game.PlayerReadyRequest
import java.util.concurrent.ConcurrentHashMap

class PlayerStatusHandler(private val playerIds: List<Int>) {
    private val statuses = ConcurrentHashMap<Int, Boolean>()

    init {
        playerIds.forEach { playerId ->
            statuses[playerId] = false
        }
    }

    fun isAllReady(): Boolean {
        return statuses.values.all { it }
    }

    fun setPlayerReady(playerReady: PlayerReadyRequest) {
        if (playerReady.playerId !in playerIds) {
            throw IllegalArgumentException("ID игрока не принадлежит этой комнате")
        }

        statuses[playerReady.playerId] = true
    }
}
