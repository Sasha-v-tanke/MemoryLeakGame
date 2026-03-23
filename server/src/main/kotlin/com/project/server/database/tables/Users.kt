package com.project.server.database.tables

import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val email = varchar("email", 100)
    val password = varchar("password", 50)

    override val primaryKey = PrimaryKey(id)
}