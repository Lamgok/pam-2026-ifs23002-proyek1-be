package org.delcom.entities

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class Ethnography(
    var id: String = UUID.randomUUID().toString(),
    var userId: String,
    var tribeName: String,
    var region: String,
    var language: String? = null,
    var traditionalHouse: String? = null,
    var traditionalWeapon: String? = null,
    var beliefSystem: String? = null,
    var description: String,
    var imageUrl: String? = null,

    @Contextual
    val createdAt: Instant = Clock.System.now(),
    @Contextual
    var updatedAt: Instant = Clock.System.now(),
)
