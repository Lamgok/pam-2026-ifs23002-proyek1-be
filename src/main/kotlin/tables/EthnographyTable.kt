package org.delcom.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object EthnographyTable : UUIDTable("ethnographies") {
    val userId = uuid("user_id")
    val tribeName = varchar("tribe_name", 100)
    val region = varchar("region", 100)
    val language = varchar("language", 100).nullable() // Bahasa daerah
    val traditionalHouse = varchar("traditional_house", 100).nullable() // Rumah adat
    val traditionalWeapon = varchar("traditional_weapon", 100).nullable() // Senjata tradisional
    val beliefSystem = varchar("belief_system", 100).nullable() // Sistem kepercayaan
    val description = text("description") // Deskripsi umum/budaya
    val imageUrl = text("image_url").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}