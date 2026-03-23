package com.project.server.service

import com.project.server.models.PlayerSession
import java.util.concurrent.ConcurrentHashMap

object SessionManager {
    private val sessions = ConcurrentHashMap<String, PlayerSession>()

    fun addSession(session: PlayerSession) {
        sessions[session.sessionId] = session
    }

    fun removeSession(sessionId: String) {
        sessions.remove(sessionId)
    }

    fun getSession(playerId: Int): PlayerSession? {
        return sessions.filter { it.value.playerId == playerId }.values.firstOrNull()
    }
}