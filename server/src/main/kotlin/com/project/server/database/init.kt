package com.project.server.database

import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.initDatabase() {
    DatabaseFactory.init(this)
    transaction {
        SchemaUtils.create(Users)
    }
}