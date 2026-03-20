package org.delcom

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import kotlinx.serialization.json.Json
import org.delcom.helpers.JWTConstants
import org.delcom.helpers.configureDatabases
import org.delcom.module.appModule
import org.koin.ktor.plugin.Koin

fun main(args: Array<String>) {
    // Memuat variabel lingkungan dari file .env
    val dotenv = dotenv {
        directory = "."
        ignoreIfMissing = false
    }

    dotenv.entries().forEach {
        System.setProperty(it.key, it.value)
    }

    EngineMain.main(args)
}

fun Application.module() {
    // Mengambil secret JWT dari konfigurasi
    val jwtSecret = environment.config.property("ktor.jwt.secret").getString()

    // Konfigurasi Autentikasi JWT (Tetap sama untuk User)
    install(Authentication) {
        jwt(JWTConstants.NAME) {
            realm = JWTConstants.REALM

            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(JWTConstants.ISSUER)
                    .withAudience(JWTConstants.AUDIENCE)
                    .build()
            )

            validate { credential ->
                val userId = credential.payload
                    .getClaim("userId")
                    .asString()

                if (!userId.isNullOrBlank())
                    JWTPrincipal(credential.payload)
                else null
            }

            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    mapOf(
                        "status" to "error",
                        "message" to "Token tidak valid"
                    )
                )
            }
        }
    }

    // Mengizinkan akses dari host mana pun (CORS)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }

    // Konfigurasi JSON Serialization
    install(ContentNegotiation) {
        json(
            Json {
                explicitNulls = false
                prettyPrint = true
                ignoreUnknownKeys = true
            }
        )
    }

    // Injeksi Dependensi menggunakan Koin
    install(Koin) {
        // appModule sekarang akan berisi EthnographyRepository & Service
        modules(appModule(jwtSecret))
    }

    // Inisialisasi Database dan Routing
    configureDatabases()
    configureRouting()
}