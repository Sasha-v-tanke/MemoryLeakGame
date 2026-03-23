package com.project.server.repository

import com.project.server.database.tables.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import com.project.server.models.User
import org.jetbrains.exposed.sql.ResultRow


object UserRepository {
    fun findByUsername(username: String): User? {
        return transaction {
            Users.select { Users.name eq username }.map { toUser(it) }.singleOrNull()
        }
    }

    fun addUser(user: User): User {
        var generatedId: Int? = null
        transaction {
            generatedId = Users.insert {
                it[name] = user.name
                it[email] = user.email
                it[password] = user.password
            } get Users.id
        }
        return user.copy(id = generatedId)
    }

    fun toUser(row: ResultRow): User {
        return User(
            id = row[Users.id],
            name = row[Users.name],
            email = row[Users.email],
            password = row[Users.password]
        )
    }
}