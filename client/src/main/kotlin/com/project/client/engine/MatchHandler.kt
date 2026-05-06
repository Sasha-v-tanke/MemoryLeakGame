package com.project.client.engine

import com.project.shared.api.events.MatchFoundEvent

class MatchHandler {
    private var opponentId: Int? = null
    private var playerIndex: Int? = null
    private var roomId: String? = null

    fun getPlayerIndex(): Int {
        return playerIndex ?: error("Match is not initialized")
    }

    fun getOpponentId(): Int {
        return opponentId ?: error("Match is not initialized")
    }

    fun getRoomId(): String {
        return roomId ?: error("Match is not initialized")
    }

    fun hasMatch(): Boolean {
        return roomId != null && opponentId != null && playerIndex != null
    }

    fun setMatch(match: MatchFoundEvent) {
        roomId = match.roomId
        opponentId = match.opponentId
        playerIndex = match.index
    }

    fun clear() {
        roomId = null
        opponentId = null
        playerIndex = null
    }
}
