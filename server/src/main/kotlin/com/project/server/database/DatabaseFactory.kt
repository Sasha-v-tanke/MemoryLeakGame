package com.project.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {

    fun init(application: Application) {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://localhost:5432/game"
            driverClassName = "org.postgresql.Driver"
            username = "gameuser"
            password = "password"
            maximumPoolSize = 10
        }

        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
    }
}