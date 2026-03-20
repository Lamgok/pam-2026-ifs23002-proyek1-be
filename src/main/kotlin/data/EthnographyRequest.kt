package org.delcom.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import org.delcom.entities.Ethnography

@Serializable
data class EthnographyRequest(
    var tribeName: String = "",
    var region: String = "",
    var language: String? = null,
    var traditionalHouse: String? = null,
    var traditionalWeapon: String? = null,
    var beliefSystem: String? = null,
    var description: String = "",
    var imageUrl: String? = null,
) {
    // Digunakan untuk validasi di ValidatorHelper
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "tribeName" to tribeName,
            "region" to region,
            "language" to language,
            "traditionalHouse" to traditionalHouse,
            "traditionalWeapon" to traditionalWeapon,
            "beliefSystem" to beliefSystem,
            "description" to description
        )
    }

    // Mengonversi request menjadi entity untuk disimpan ke database
    fun toEntity(userId: String): Ethnography {
        return Ethnography(
            userId = userId,
            tribeName = tribeName,
            region = region,
            language = language,
            traditionalHouse = traditionalHouse,
            traditionalWeapon = traditionalWeapon,
            beliefSystem = beliefSystem,
            description = description,
            imageUrl = imageUrl,
            updatedAt = Clock.System.now()
        )
    }
}