package org.delcom.repositories

import org.delcom.dao.RefreshTokenDAO
import org.delcom.entities.RefreshToken
import org.delcom.helpers.parseUuidOrThrow
import org.delcom.helpers.refreshTokenDAOToModel
import org.delcom.helpers.suspendTransaction
import org.delcom.tables.RefreshTokenTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere

class RefreshTokenRepository : IRefreshTokenRepository {
    override suspend fun getByToken(refreshToken: String, authToken: String): RefreshToken? = suspendTransaction {
        RefreshTokenDAO
            .find { (RefreshTokenTable.refreshToken eq refreshToken) and (RefreshTokenTable.authToken eq authToken) }
            .limit(1)
            .map(::refreshTokenDAOToModel)
            .firstOrNull()
    }

    override suspend fun create(newRefreshToken: RefreshToken): String = suspendTransaction {
        val refreshToken = RefreshTokenDAO.new {
            userId = parseUuidOrThrow(newRefreshToken.userId, "ID user")
            refreshToken = newRefreshToken.refreshToken
            authToken = newRefreshToken.authToken
            createdAt = newRefreshToken.createdAt
        }

        refreshToken.id.value.toString()
    }

    override suspend fun delete(authToken: String): Boolean = suspendTransaction {
        val rowsDeleted = RefreshTokenTable.deleteWhere {
            RefreshTokenTable.authToken eq authToken
        }
        rowsDeleted >= 1
    }

    override suspend fun deleteByUserId(userId: String): Boolean = suspendTransaction {
        val rowsDeleted = RefreshTokenTable.deleteWhere {
            RefreshTokenTable.userId eq parseUuidOrThrow(userId, "ID user")
        }
        rowsDeleted >= 1
    }

}
