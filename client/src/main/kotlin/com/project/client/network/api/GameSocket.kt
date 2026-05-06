package com.project.client.network.api

import com.badlogic.gdx.Gdx
import com.project.shared.api.game.GameRequest
import com.project.shared.api.game.GameResponse
import com.project.shared.api.game.PlayCardRequest
import com.project.shared.api.game.PlayCardResponse
import com.project.shared.api.game.PlayerReadyRequest
import com.project.shared.api.game.PlayerReadyResponse
import com.project.shared.engine.entities.units.UnitType
import kotlinx.coroutines.launch

class GameSocket : WebSocket("game") {
    fun sendPlayerReady(playerId: Int, roomId: String, onResult: (GameResponse) -> Unit) {
        scope.launch {
            try {
                send<GameRequest>(PlayerReadyRequest(playerId, roomId))

                val response = receiveMessage<PlayerReadyResponse>()
                    ?: PlayerReadyResponse(false, "Server response timeout")

                Gdx.app.postRunnable {
                    onResult(response)
                }
            } catch (e: Exception) {
                Gdx.app.error("GameSocket", "Ready request failed: ${e.message}", e)

                Gdx.app.postRunnable {
                    onResult(
                        PlayerReadyResponse(
                            success = false,
                            description = "Connection error: ${e.message}"
                        )
                    )
                }
            }
        }
    }

    fun playCard(
        playerId: Int,
        roomId: String,
        unitType: UnitType,
        targetX: Float,
        targetY: Float,
        onResult: (PlayCardResponse) -> Unit
    ) {
        scope.launch {
            try {
                send<GameRequest>(
                    PlayCardRequest(
                        playerId = playerId,
                        roomId = roomId,
                        unitType = unitType,
                        targetX = targetX,
                        targetY = targetY
                    )
                )

                val response = receiveMessage<PlayCardResponse>()
                    ?: PlayCardResponse(false, "Server response timeout")

                Gdx.app.postRunnable {
                    onResult(response)
                }
            } catch (e: Exception) {
                Gdx.app.error("GameSocket", "Play card failed: ${e.message}", e)

                Gdx.app.postRunnable {
                    onResult(
                        PlayCardResponse(
                            success = false,
                            description = "Connection error: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}
