package com.project.client

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.project.client.engine.MatchHandler
import com.project.client.engine.TextureCache
import com.project.client.network.api.ListenSocket
import com.project.client.ui.screens.GameScreen
import com.project.client.ui.screens.LoginScreen
import com.project.client.ui.screens.MatchMakingScreen
import com.project.shared.api.events.Event
import com.project.shared.api.events.GameOverEvent
import com.project.shared.api.events.GameStartEvent
import com.project.shared.api.events.GameStateSnapshotEvent
import com.project.shared.api.events.MatchFoundEvent
import com.project.shared.api.events.SystemMessageEvent
import com.project.shared.engine.entities.units.UnitRegistry
import com.project.shared.engine.entities.units.UnitType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyGame : Game() {
    companion object {
        const val DECK_SIZE = 10
        const val MAX_CARD_COPIES = 2
    }

    private var playerId: Int? = null
    private var username: String = "Player"
    private var selectedDeck: MutableList<UnitType> = UnitRegistry.defaultDeck.toMutableList()

    val matchHandler = MatchHandler()

    private lateinit var listenSocket: ListenSocket
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun create() {
        setScreen(LoginScreen(this))
    }

    fun setProfile(playerId: Int, username: String) {
        this.playerId = playerId
        this.username = username
        this.selectedDeck = loadDeck(playerId).toMutableList()

        if (::listenSocket.isInitialized) {
            listenSocket.close()
        }

        listenSocket = ListenSocket(playerId)
        listenSocket.onEvent = { event -> onEvent(event) }

        scope.launch {
            listenSocket.makeHandshake()
        }
    }

    fun getPlayerId(): Int {
        return playerId ?: error("Player is not logged in")
    }

    fun getUsername(): String {
        return username
    }

    fun getSelectedDeck(): List<UnitType> {
        return selectedDeck.ifEmpty { UnitRegistry.defaultDeck }.toList()
    }

    fun setSelectedDeck(deck: List<UnitType>) {
        val validTypes = UnitRegistry.all().map { it.unitType }.toSet()
        val copyCounts = mutableMapOf<UnitType, Int>()
        val normalized = mutableListOf<UnitType>()

        deck.filter { it in validTypes }.forEach { unitType ->
            if (normalized.size >= DECK_SIZE) return@forEach
            val copies = copyCounts.getOrDefault(unitType, 0)
            if (copies < MAX_CARD_COPIES) {
                copyCounts[unitType] = copies + 1
                normalized += unitType
            }
        }

        selectedDeck = normalized.ifEmpty { UnitRegistry.defaultDeck.toMutableList() }
        saveDeck()
    }

    fun resetDeck() {
        selectedDeck = UnitRegistry.defaultDeck.toMutableList()
        saveDeck()
    }

    fun returnToMainMenu() {
        matchHandler.clear()
        setScreen(com.project.client.ui.screens.MainScreen(this))
    }

    private fun loadDeck(playerId: Int): List<UnitType> {
        val raw = Gdx.app.getPreferences("memory-leak-arena").getString(deckKey(playerId), "")
        if (raw.isBlank()) return UnitRegistry.defaultDeck

        val loaded = raw.split(",")
            .mapNotNull { value ->
                try {
                    UnitType.valueOf(value)
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            .take(DECK_SIZE)

        return loaded.ifEmpty { UnitRegistry.defaultDeck }
    }

    private fun saveDeck() {
        val playerId = playerId ?: return
        val encoded = selectedDeck.joinToString(",") { it.name }
        Gdx.app.getPreferences("memory-leak-arena").putString(deckKey(playerId), encoded).flush()
    }

    private fun deckKey(playerId: Int): String {
        return "deck.$playerId"
    }

    private fun onEvent(event: Event) {
        Gdx.app.postRunnable {
            when (event) {
                is MatchFoundEvent -> (screen as? MatchMakingScreen)?.startGame(event)
                is GameStartEvent -> (screen as? GameScreen)?.startGame(event)
                is GameStateSnapshotEvent -> (screen as? GameScreen)?.updateGameState(event)
                is GameOverEvent -> (screen as? GameScreen)?.finishGame(event)
                is SystemMessageEvent -> (screen as? GameScreen)?.showSystemMessage(event.message)
            }
        }
    }

    override fun dispose() {
        if (::listenSocket.isInitialized) {
            listenSocket.close()
        }
        TextureCache.dispose()
        super.dispose()
    }
}
