package com.project.server.database.tables

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50).uniqueIndex()
    val email = varchar("email", 100)
    val password = varchar("password", 255)

    override val primaryKey = PrimaryKey(id)
}
