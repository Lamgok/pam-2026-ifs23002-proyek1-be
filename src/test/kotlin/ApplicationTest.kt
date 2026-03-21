package org.delcom

import io.ktor.client.request.forms.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.content.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.delcom.entities.Ethnography
import org.delcom.entities.RefreshToken
import org.delcom.entities.User
import org.delcom.repositories.IEthnographyRepository
import org.delcom.repositories.IRefreshTokenRepository
import org.delcom.repositories.IUserRepository
import org.delcom.services.AuthService
import org.delcom.services.EthnographyService
import org.delcom.services.UserService
import org.koin.dsl.module
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun testRoot() = testApplication {
        application {
            module(connectDatabase = false)
        }
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun invalidUserIdReturns400() = testApplication {
        application {
            module(connectDatabase = false, appModules = listOf(testModule()))
        }

        val response = client.get("/images/users/not-a-uuid")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("ID user tidak valid"))
    }

    @Test
    fun cannotDeleteEthnographyOwnedByAnotherUser() = testApplication {
        application {
            module(connectDatabase = false, appModules = listOf(testModule()))
        }

        val ownerLogin = login("owner")
        val createdId = createEthnography(ownerLogin.authToken)

        val intruderToken = login("intruder").authToken
        val response = client.delete("/ethnographies/$createdId") {
            bearerAuth(intruderToken)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("Anda tidak memiliki akses"))
    }

    @Test
    fun rejectNonImageEthnographyUpload() = testApplication {
        application {
            module(connectDatabase = false, appModules = listOf(testModule()))
        }

        val login = login("uploader")
        val createdId = createEthnography(login.authToken)

        val response = client.put("/ethnographies/$createdId/image") {
            bearerAuth(login.authToken)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            "file",
                            "plain-text".toByteArray(),
                            Headers.build {
                                append(HttpHeaders.ContentDisposition, """form-data; name="file"; filename="bad.txt"""")
                                append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                            }
                        )
                    }
                )
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Format file gambar tidak didukung"))
    }

    @Test
    fun authAndEthnographyFlowWorks() = testApplication {
        application {
            module(connectDatabase = false, appModules = listOf(testModule()))
        }

        val firstLogin = login("flow")
        val createResponse = client.post("/ethnographies") {
            bearerAuth(firstLogin.authToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "tribeName": "Suku A",
                  "region": "Region A",
                  "description": "Deskripsi A"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, createResponse.status)

        val createdId = json.decodeFromString(IdResponse.serializer(), createResponse.bodyAsText()).data.ethnographyId

        val refreshResponse = client.post("/auth/refresh-token") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "refreshToken": "${firstLogin.refreshToken}",
                  "authToken": "${firstLogin.authToken}"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, refreshResponse.status)

        val refreshed = json.decodeFromString(LoginResponse.serializer(), refreshResponse.bodyAsText()).data

        val updateResponse = client.put("/ethnographies/$createdId") {
            bearerAuth(refreshed.authToken)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "tribeName": "Suku A Updated",
                  "region": "Region B",
                  "description": "Deskripsi B"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)

        val meResponse = client.get("/users/me") {
            bearerAuth(refreshed.authToken)
        }
        assertEquals(HttpStatusCode.OK, meResponse.status)

        val logoutResponse = client.post("/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "authToken": "${refreshed.authToken}"
                }
                """.trimIndent()
            )
        }
        assertEquals(HttpStatusCode.OK, logoutResponse.status)
    }

    private suspend fun ApplicationTestBuilder.login(username: String): LoginTokens {
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "name": "$username",
                  "username": "$username",
                  "password": "secret123"
                }
                """.trimIndent()
            )
        }
        assertTrue(registerResponse.status == HttpStatusCode.OK || registerResponse.status == HttpStatusCode.Conflict)

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "username": "$username",
                  "password": "secret123"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        return json.decodeFromString(LoginResponse.serializer(), response.bodyAsText()).data
    }

    private suspend fun ApplicationTestBuilder.createEthnography(token: String): String {
        val response = client.post("/ethnographies") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "tribeName": "Suku Test",
                  "region": "Region Test",
                  "description": "Deskripsi Test"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        return json.decodeFromString(IdResponse.serializer(), response.bodyAsText()).data.ethnographyId
    }

    private fun testModule() = module {
        single<IUserRepository> { InMemoryUserRepository() }
        single<IRefreshTokenRepository> { InMemoryRefreshTokenRepository() }
        single<IEthnographyRepository> { InMemoryEthnographyRepository() }
        single { UserService(get(), get()) }
        single { AuthService("dev-secret", get(), get()) }
        single { EthnographyService(get(), get()) }
    }
}

@Serializable
private data class LoginResponse(val data: LoginTokens)

@Serializable
private data class LoginTokens(val authToken: String, val refreshToken: String)

@Serializable
private data class IdResponse(val data: EthnographyIdPayload)

@Serializable
private data class EthnographyIdPayload(val ethnographyId: String)

private class InMemoryUserRepository : IUserRepository {
    private val users = linkedMapOf<String, User>()

    override suspend fun getById(userId: String): User? {
        if (runCatching { UUID.fromString(userId) }.isFailure) {
            throw org.delcom.data.AppException(400, "ID user tidak valid!")
        }
        return users[userId]
    }

    override suspend fun getByUsername(username: String): User? = users.values.firstOrNull { it.username == username }

    override suspend fun create(user: User): String {
        users[user.id] = user
        return user.id
    }

    override suspend fun update(id: String, newUser: User): Boolean {
        if (!users.containsKey(id)) return false
        users[id] = newUser.copy(id = id, updatedAt = Clock.System.now())
        return true
    }

    override suspend fun delete(id: String): Boolean = users.remove(id) != null
}

private class InMemoryRefreshTokenRepository : IRefreshTokenRepository {
    private val tokens = linkedMapOf<String, RefreshToken>()

    override suspend fun getByToken(refreshToken: String, authToken: String): RefreshToken? =
        tokens.values.firstOrNull { it.refreshToken == refreshToken && it.authToken == authToken }

    override suspend fun create(newRefreshToken: RefreshToken): String {
        tokens[newRefreshToken.id] = newRefreshToken
        return newRefreshToken.id
    }

    override suspend fun delete(authToken: String): Boolean {
        val removed = tokens.values.removeIf { it.authToken == authToken }
        return removed
    }

    override suspend fun deleteByUserId(userId: String): Boolean {
        val removed = tokens.values.removeIf { it.userId == userId }
        return removed
    }
}

private class InMemoryEthnographyRepository : IEthnographyRepository {
    private val items = linkedMapOf<String, Ethnography>()

    override suspend fun getAll(userId: String, search: String, page: Int, perPage: Int): List<Ethnography> {
        val filtered = items.values.filter {
            it.userId == userId && (
                search.isBlank() ||
                    it.tribeName.contains(search, ignoreCase = true) ||
                    it.region.contains(search, ignoreCase = true) ||
                    (it.language?.contains(search, ignoreCase = true) == true)
                )
        }
        val startIndex = ((page - 1).coerceAtLeast(0)) * perPage.coerceAtLeast(1)
        return filtered.drop(startIndex).take(perPage.coerceAtLeast(1))
    }

    override suspend fun getById(id: String): Ethnography? {
        if (runCatching { UUID.fromString(id) }.isFailure) {
            throw org.delcom.data.AppException(400, "ID etnografi tidak valid!")
        }
        return items[id]
    }

    override suspend fun create(ethnography: Ethnography): String {
        items[ethnography.id] = ethnography
        return ethnography.id
    }

    override suspend fun update(userId: String, id: String, newEthnography: Ethnography): Boolean {
        val current = items[id] ?: return false
        if (current.userId != userId) return false
        items[id] = newEthnography.copy(id = id, updatedAt = Clock.System.now())
        return true
    }

    override suspend fun delete(userId: String, id: String): Boolean {
        val current = items[id] ?: return false
        if (current.userId != userId) return false
        return items.remove(id) != null
    }
}
