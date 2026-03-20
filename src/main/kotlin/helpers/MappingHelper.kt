package org.delcom.helpers

import kotlinx.coroutines.Dispatchers
import org.delcom.dao.EthnographyDAO
import org.delcom.dao.RefreshTokenDAO
import org.delcom.dao.UserDAO
import org.delcom.entities.Ethnography
import org.delcom.entities.RefreshToken
import org.delcom.entities.User
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> suspendTransaction(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

fun userDAOToModel(dao: UserDAO) = User(
    dao.id.value.toString(),
    dao.name,
    dao.username,
    dao.password,
    dao.photo,
    dao.about,
    dao.createdAt,
    dao.updatedAt
).apply { about = dao.about }

fun refreshTokenDAOToModel(dao: RefreshTokenDAO) = RefreshToken(
    dao.id.value.toString(),
    dao.userId.toString(),
    dao.refreshToken,
    dao.authToken,
    dao.createdAt,
)

fun ethnographyDAOToModel(dao: EthnographyDAO) = Ethnography(
    id = dao.id.value.toString(),
    userId = dao.userId.toString(),
    tribeName = dao.tribeName,
    region = dao.region,
    language = dao.language,
    traditionalHouse = dao.traditionalHouse,
    traditionalWeapon = dao.traditionalWeapon,
    beliefSystem = dao.beliefSystem,
    description = dao.description,
    imageUrl = dao.imageUrl,
    createdAt = dao.createdAt,
    updatedAt = dao.updatedAt
)
