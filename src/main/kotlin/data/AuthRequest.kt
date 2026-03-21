package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.User

@Serializable
data class AuthRequest(
    var name: String = "",
    var username: String = "",
    var password: String = "",
    var newPassword: String = "",
    var about: String? = null,
){
    fun normalizedUsername(): String = username.trim().lowercase()

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "username" to normalizedUsername(),
            "password" to password,
            "newPassword" to newPassword,
            "about" to about
        )
    }

    fun toEntity(): User {
        return User(
            name = name,
            username = normalizedUsername(),
            password = password,
            updatedAt = Clock.System.now()
        )
    }

}
