package org.delcom.repositories

import org.delcom.dao.EthnographyDAO
import org.delcom.entities.Ethnography
import org.delcom.helpers.ethnographyDAOToModel
import org.delcom.helpers.suspendTransaction
import org.delcom.tables.EthnographyTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import java.util.UUID

class EthnographyRepository : IEthnographyRepository {

    override suspend fun getAll(
        userId: String,
        search: String,
        page: Int,
        perPage: Int
    ): List<Ethnography> = suspendTransaction {
        val query = if (search.isBlank()) {
            EthnographyDAO.find {
                EthnographyTable.userId eq UUID.fromString(userId)
            }
        } else {
            val keyword = "%${search.lowercase()}%"
            EthnographyDAO.find {
                (EthnographyTable.userId eq UUID.fromString(userId)) and (
                    (EthnographyTable.tribeName.lowerCase() like keyword) or
                        (EthnographyTable.region.lowerCase() like keyword) or
                        (EthnographyTable.language.lowerCase() like keyword)
                    )
            }
        }

        query.orderBy(EthnographyTable.createdAt to SortOrder.DESC)
            .limit(perPage)
            .offset(((page - 1) * perPage).toLong())
            .map(::ethnographyDAOToModel)
    }

    override suspend fun getById(id: String): Ethnography? = suspendTransaction {
        EthnographyDAO.find { EthnographyTable.id eq UUID.fromString(id) }
            .limit(1)
            .map(::ethnographyDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(ethnography: Ethnography): String = suspendTransaction {
        val newEntry = EthnographyDAO.new {
            userId = UUID.fromString(ethnography.userId)
            tribeName = ethnography.tribeName
            region = ethnography.region
            language = ethnography.language
            traditionalHouse = ethnography.traditionalHouse
            traditionalWeapon = ethnography.traditionalWeapon
            beliefSystem = ethnography.beliefSystem
            description = ethnography.description
            imageUrl = ethnography.imageUrl
            createdAt = ethnography.createdAt
            updatedAt = ethnography.updatedAt
        }
        newEntry.id.value.toString()
    }

    override suspend fun update(userId: String, id: String, newEthnography: Ethnography): Boolean = suspendTransaction {
        val entry = EthnographyDAO.find {
            (EthnographyTable.id eq UUID.fromString(id)) and
                (EthnographyTable.userId eq UUID.fromString(userId))
        }
            .limit(1)
            .firstOrNull()

        if (entry != null) {
            entry.tribeName = newEthnography.tribeName
            entry.region = newEthnography.region
            entry.language = newEthnography.language
            entry.traditionalHouse = newEthnography.traditionalHouse
            entry.traditionalWeapon = newEthnography.traditionalWeapon
            entry.beliefSystem = newEthnography.beliefSystem
            entry.description = newEthnography.description
            entry.imageUrl = newEthnography.imageUrl
            entry.updatedAt = newEthnography.updatedAt
            true
        } else {
            false
        }
    }

    override suspend fun delete(userId: String, id: String): Boolean = suspendTransaction {
        val rowsDeleted = EthnographyTable.deleteWhere {
            (EthnographyTable.id eq UUID.fromString(id)) and
                (EthnographyTable.userId eq UUID.fromString(userId))
        }
        rowsDeleted >= 1
    }
}
