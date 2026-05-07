package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.game.BuildFactoryRequest
import com.project.shared.api.game.BuildFactoryResponse
import com.project.shared.api.game.ForfeitMatchRequest
import com.project.shared.api.game.ForfeitMatchResponse
import com.project.shared.api.game.GameRequest
import com.project.shared.api.game.GameResponse
import com.project.shared.api.game.PlayCardRequest
import com.project.shared.api.game.PlayCardResponse
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.api.game.PlayerReadyResponse
import com.project.shared.engine.entities.components.FactoryType
import com.project.shared.engine.entities.units.UnitType
import kotlinx.coroutines.launch

class GameSocket : WebSocket("game") {
    fun sendPlayerReady(playerId: Int, roomId: String, onResult: (GameResponse) -> Unit) {
        scope.launch {
            try {
                send<GameRequest>(PlayerReadyRequest(playerId, roomId))
                val response = receiveMessage<PlayerReadyResponse>() ?: PlayerReadyResponse(false, "Server response timeout")
                Gdx.app.postRunnable { onResult(response) }
            } catch (e: Exception) {
                Gdx.app.postRunnable { onResult(PlayerReadyResponse(false, "Connection error: ${e.message}")) }
            }
        }
    }

    fun playCard(playerId: Int, roomId: String, unitType: UnitType, targetX: Float, targetY: Float, onResult: (PlayCardResponse) -> Unit) {
        scope.launch {
            try {
                send<GameRequest>(PlayCardRequest(playerId, roomId, unitType, targetX, targetY))
                val response = receiveMessage<PlayCardResponse>() ?: PlayCardResponse(false, "Server response timeout")
                Gdx.app.postRunnable { onResult(response) }
            } catch (e: Exception) {
                Gdx.app.postRunnable { onResult(PlayCardResponse(false, "Connection error: ${e.message}")) }
            }
        }
    }

    fun buildFactory(playerId: Int, roomId: String, factoryType: FactoryType, onResult: (BuildFactoryResponse) -> Unit) {
        scope.launch {
            try {
                send<GameRequest>(BuildFactoryRequest(playerId, roomId, factoryType))
                val response = receiveMessage<BuildFactoryResponse>() ?: BuildFactoryResponse(false, "Server response timeout")
                Gdx.app.postRunnable { onResult(response) }
            } catch (e: Exception) {
                Gdx.app.postRunnable { onResult(BuildFactoryResponse(false, "Connection error: ${e.message}")) }
            }
        }
    }

    fun forfeit(playerId: Int, roomId: String, onResult: (ForfeitMatchResponse) -> Unit) {
        scope.launch {
            try {
                send<GameRequest>(ForfeitMatchRequest(playerId, roomId))
                val response = receiveMessage<ForfeitMatchResponse>() ?: ForfeitMatchResponse(false, "Server response timeout")
                Gdx.app.postRunnable { onResult(response) }
            } catch (e: Exception) {
                Gdx.app.postRunnable { onResult(ForfeitMatchResponse(false, "Connection error: ${e.message}")) }
            }
        }
    }
}
