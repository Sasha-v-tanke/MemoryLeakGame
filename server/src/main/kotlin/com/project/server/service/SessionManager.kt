package com.project.server.service

import com.project.server.models.PlayerSession
import java.util.concurrent.ConcurrentHashMap

object SessionManager {
    private val sessionsById = ConcurrentHashMap<String, PlayerSession>()
    private val sessionIdByPlayerId = ConcurrentHashMap<Int, String>()

    fun addSession(session: PlayerSession) {
        sessionIdByPlayerId[session.playerId]?.let { oldSessionId ->
            sessionsById.remove(oldSessionId)
        }

        sessionsById[session.sessionId] = session
        sessionIdByPlayerId[session.playerId] = session.sessionId
    }

    fun removeSession(sessionId: String) {
        val removed = sessionsById.remove(sessionId)
        if (removed != null) {
            sessionIdByPlayerId.remove(removed.playerId)
        }
    }

    fun getSession(playerId: Int): PlayerSession? {
        val sessionId = sessionIdByPlayerId[playerId] ?: return null
        return sessionsById[sessionId]
    }

    fun getSessionById(sessionId: String): PlayerSession? {
        return sessionsById[sessionId]
    }
}
