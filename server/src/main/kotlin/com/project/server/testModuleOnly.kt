package com.project.server

import com.project.server.routing.testModule
import io.ktor.server.application.Application

fun Application.testModuleOnly() {
    testModule()
}
