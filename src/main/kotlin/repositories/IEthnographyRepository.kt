package org.delcom.repositories

import org.delcom.entities.Ethnography

interface IEthnographyRepository {
    suspend fun getAll(search: String, page: Int, perPage: Int): List<Ethnography>
    suspend fun getById(id: String): Ethnography?
    suspend fun create(ethnography: Ethnography): String
    suspend fun update(id: String, newEthnography: Ethnography): Boolean
    suspend fun delete(id: String): Boolean
}