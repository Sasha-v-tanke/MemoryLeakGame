package com.project.client.engine

import com.project.shared.api.events.MatchFoundEvent

class MatchHandler {
    private var opponentID: Int? = null
    private var playerIndex: Int? = null
    private var roomID: String? = null

    fun getPlayerIndex(): Int {
        return playerIndex!!
    }

    fun getOpponentId(): Int {
        return opponentID!!
    }

    fun getRoomId(): String {
        return roomID!!
    }

    fun setMatch(match: MatchFoundEvent) {
        roomID = match.roomId
        opponentID = match.opponentId
        playerIndex = match.index
    }
}