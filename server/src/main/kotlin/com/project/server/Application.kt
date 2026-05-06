package com.project.server

import com.project.server.database.initDatabase
import com.project.server.routing.authModule
import com.project.server.routing.gameModule
import com.project.server.routing.matchMakingModule
import com.project.server.routing.testModule
import com.project.shared.api.JsonFormats
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(JsonFormats.default)
    }

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 30.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    initDatabase()

    testModule()
    authModule()
    matchMakingModule()
    gameModule()
}
