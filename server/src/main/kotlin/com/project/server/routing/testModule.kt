package com.project.server.routing

import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing


fun Application.testModule() {
    routing {
        get("/ping") {
            call.respondText("pong")
        }
    }
}