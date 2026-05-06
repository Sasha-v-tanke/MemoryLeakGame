package com.project.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(application: Application) {
        val jdbcUrl = System.getenv("MEMORY_LEAK_DB_URL")
            ?: "jdbc:postgresql://localhost:5432/game"

        val username = System.getenv("MEMORY_LEAK_DB_USER")
            ?: "gameuser"

        val password = System.getenv("MEMORY_LEAK_DB_PASSWORD")
            ?: "password"

        val config = HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            driverClassName = "org.postgresql.Driver"
            this.username = username
            this.password = password
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)

        application.log.info("Database connected: $jdbcUrl")
    }
}
