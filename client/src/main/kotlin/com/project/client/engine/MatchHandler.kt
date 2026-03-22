package com.project.client.engine

import com.project.shared.api.MatchFound

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

    fun setMatch(match: MatchFound) {
        roomID = match.roomId
        opponentID = match.opponentId
        playerIndex = match.index
    }
}