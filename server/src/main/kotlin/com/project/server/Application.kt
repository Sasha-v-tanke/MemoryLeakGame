package com.project.server

import com.project.server.database.DatabaseFactory
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.receive
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val username: String,
    val score: Int
)


private val players = mutableListOf<Player>()
private var idCounter = 1

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    initDatabase()
}

fun Application.initDatabase() {
    DatabaseFactory.init(this)
}


fun Application.testModule() {
    routing {
        get("/ping") {
            call.respondText("pong")
        }
    }

    routing {
        post("/players") {
            val request = call.receive<Player>()
            val newPlayer = request.copy(id = idCounter++)
            players.add(newPlayer)
            call.respond(newPlayer)
        }

        get("/players") {
            call.respond(players)
        }
    }
}
