package org.delcom.dao

import org.delcom.tables.EthnographyTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import java.util.UUID

class EthnographyDAO(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, EthnographyDAO>(EthnographyTable)

    var userId by EthnographyTable.userId
    var tribeName by EthnographyTable.tribeName
    var region by EthnographyTable.region
    var language by EthnographyTable.language
    var traditionalHouse by EthnographyTable.traditionalHouse
    var traditionalWeapon by EthnographyTable.traditionalWeapon
    var beliefSystem by EthnographyTable.beliefSystem
    var description by EthnographyTable.description
    var imageUrl by EthnographyTable.imageUrl
    var createdAt by EthnographyTable.createdAt
    var updatedAt by EthnographyTable.updatedAt
}