package org.delcom

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.auth.*
import org.delcom.helpers.JWTConstants
import org.delcom.services.AuthService
import org.delcom.services.EthnographyService
import org.delcom.services.UserService
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val authService by inject<AuthService>()
    val userService by inject<UserService>()
    val ethnographyService by inject<EthnographyService>()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        route("/auth") {
            post("/register") {
                authService.postRegister(call)
            }
            post("/login") {
                authService.postLogin(call)
            }
            post("/refresh-token") {
                authService.postRefreshToken(call)
            }
            post("/logout") {
                authService.postLogout(call)
            }
        }

        authenticate(JWTConstants.NAME) {
            route("/users") {
                get("/me") {
                    userService.getMe(call)
                }
                put("/me") {
                    userService.putMe(call)
                }
                put("/me/password") {
                    userService.putMyPassword(call)
                }
                put("/me/photo") {
                    userService.putMyPhoto(call)
                }
            }

            route("/ethnographies") {
                get {
                    ethnographyService.getAll(call)
                }
                get("/{id}") {
                    ethnographyService.getById(call)
                }
                post {
                    ethnographyService.post(call)
                }
                put("/{id}") {
                    ethnographyService.put(call)
                }
                put("/{id}/image") {
                    ethnographyService.putImage(call)
                }
                delete("/{id}") {
                    ethnographyService.delete(call)
                }
            }
        }

        get("/images/users/{id}") {
            userService.getPhoto(call)
        }

        get("/images/ethnographies/{id}") {
            ethnographyService.getImage(call)
        }
    }
}
