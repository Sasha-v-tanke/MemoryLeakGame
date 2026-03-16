package com.project.server

import com.project.server.models.User
import com.project.server.repository.UserRepository
import com.project.server.routing.AuthResponse
import com.project.server.routing.LoginRequest
import com.project.server.routing.RegisterRequest
import com.project.server.routing.authModule
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthModuleTest {

    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setUp() {
        mockkObject(UserRepository)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(UserRepository)
    }

    @Test
    fun `register returns success for new user`() = testApplication {
        every { UserRepository.findByUsername("alice") } returns null
        every {
            UserRepository.addUser(match { it.name == "alice" && it.email == "alice@mail.com" && it.password == "1234" })
        } returns User(id = 42, name = "alice", email = "alice@mail.com", password = "1234")

        application {
            install(ContentNegotiation) {
                json()
            }
            authModule()
        }

        val response = client.post("/user/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","password":"1234","email":"alice@mail.com"}""")
        }

        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, body.success)
        assertEquals("dummy-token-42", body.token)
        assertEquals("User registered successfully", body.message)
    }

    @Test
    fun `register returns error for duplicate username`() = testApplication {
        every { UserRepository.findByUsername("alice") } returns User(
            id = 1,
            name = "alice",
            email = "alice@mail.com",
            password = "1234"
        )

        application {
            install(ContentNegotiation) { json() }
            authModule()
        }

        val response = client.post("/user/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","password":"1234","email":"alice@mail.com"}""")
        }

        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(false, body.success)
        assertNull(body.token)
        assertEquals("Username already exists", body.message)
    }

    @Test
    fun `login returns success for valid credentials`() = testApplication {
        every { UserRepository.findByUsername("alice") } returns User(
            id = 7,
            name = "alice",
            email = "alice@mail.com",
            password = "1234"
        )

        application {
            install(ContentNegotiation) { json() }
            authModule()
        }

        val response = client.post("/user/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","password":"1234"}""")
        }

        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(true, body.success)
        assertEquals("dummy-token-7", body.token)
        assertEquals("Login successful", body.message)
    }

    @Test
    fun `login returns error for unknown user`() = testApplication {
        every { UserRepository.findByUsername("ghost") } returns null

        application {
            install(ContentNegotiation) { json() }
            authModule()
        }

        val response = client.post("/user/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"ghost","password":"1234"}""")
        }

        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(false, body.success)
        assertNull(body.token)
        assertEquals("Invalid username or password", body.message)
    }

    @Test
    fun `login returns error for wrong password`() = testApplication {
        every { UserRepository.findByUsername("alice") } returns User(
            id = 7,
            name = "alice",
            email = "alice@mail.com",
            password = "1234"
        )

        application {
            install(ContentNegotiation) { json() }
            authModule()
        }

        val response = client.post("/user/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"alice","password":"wrong"}""")
        }

        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(false, body.success)
        assertNull(body.token)
        assertEquals("Invalid username or password", body.message)
    }
}