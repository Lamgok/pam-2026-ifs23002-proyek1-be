package org.delcom.services

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.delcom.data.AppException
import org.delcom.data.DataResponse
import org.delcom.data.EthnographyRequest
import org.delcom.helpers.ServiceHelper
import org.delcom.helpers.ValidatorHelper
import org.delcom.repositories.IEthnographyRepository
import org.delcom.repositories.IUserRepository
import java.io.File
import java.util.*

class EthnographyService(
    private val userRepo: IUserRepository,
    private val ethnographyRepo: IEthnographyRepository
) {
    // Mengambil semua data suku (dengan search & pagination)
    suspend fun getAll(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val search = call.request.queryParameters["search"] ?: ""
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val perPage = call.request.queryParameters["perPage"]?.toIntOrNull() ?: 10

        val ethnographies = ethnographyRepo.getAll(search, page, perPage)

        val response = DataResponse(
            "success",
            "Berhasil mengambil daftar data etnografi",
            mapOf("ethnographies" to ethnographies)
        )
        call.respond(response)
    }

    // Mengambil detail satu suku berdasarkan ID
    suspend fun getById(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")

        val data = ethnographyRepo.getById(id) ?: throw AppException(404, "Data etnografi tidak ditemukan!")

        val response = DataResponse(
            "success",
            "Berhasil mengambil detail data etnografi",
            mapOf("ethnography" to data)
        )
        call.respond(response)
    }

    // Menambah data suku baru
    suspend fun post(call: ApplicationCall) {
        val user = ServiceHelper.getAuthUser(call, userRepo)
        val request = call.receive<EthnographyRequest>()

        // Validasi input wajib
        val validator = ValidatorHelper(request.toMap())
        validator.required("tribeName", "Nama suku tidak boleh kosong")
        validator.required("region", "Wilayah tidak boleh kosong")
        validator.required("description", "Deskripsi tidak boleh kosong")
        validator.validate()

        val id = ethnographyRepo.create(request.toEntity(user.id))

        val response = DataResponse(
            "success",
            "Berhasil menambahkan data etnografi baru",
            mapOf("ethnographyId" to id)
        )
        call.respond(response)
    }

    // Memperbarui data suku
    suspend fun put(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val request = call.receive<EthnographyRequest>()
        val oldData = ethnographyRepo.getById(id) ?: throw AppException(404, "Data tidak tersedia!")

        // Pastikan hanya pemilik/pembuat yang bisa edit (Opsional, sesuai kebutuhan)
        if (oldData.userId != user.id) throw AppException(403, "Anda tidak memiliki akses!")

        val isUpdated = ethnographyRepo.update(id, request.toEntity(user.id))
        if (!isUpdated) throw AppException(400, "Gagal memperbarui data!")

        call.respond(DataResponse("success", "Berhasil mengubah data etnografi", null))
    }

    // Mengubah foto suku/budaya (Upload File)
    suspend fun putImage(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)

        var imagePath: String? = null
        val multipartData = call.receiveMultipart()
        multipartData.forEachPart { part ->
            if (part is PartData.FileItem) {
                val ext = part.originalFileName?.substringAfterLast('.', "") ?: ""
                val fileName = "${UUID.randomUUID()}.$ext"
                val filePath = "uploads/ethnographies/$fileName"

                withContext(Dispatchers.IO) {
                    val file = File(filePath)
                    file.parentFile.mkdirs()
                    part.provider().copyAndClose(file.writeChannel())
                    imagePath = filePath
                }
            }
            part.dispose()
        }

        if (imagePath == null) throw AppException(400, "Foto tidak tersedia!")

        val oldData = ethnographyRepo.getById(id) ?: throw AppException(404, "Data tidak ditemukan!")
        val newEntity = oldData.copy(imageUrl = imagePath)

        ethnographyRepo.update(id, newEntity)

        // Hapus foto lama jika ada
        oldData.imageUrl?.let { File(it).apply { if (exists()) delete() } }

        call.respond(DataResponse("success", "Berhasil mengunggah foto etnografi", null))
    }

    // Menghapus data suku
    suspend fun delete(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val user = ServiceHelper.getAuthUser(call, userRepo)

        val data = ethnographyRepo.getById(id) ?: throw AppException(404, "Data tidak ditemukan!")

        val isDeleted = ethnographyRepo.delete(id)
        if (!isDeleted) throw AppException(400, "Gagal menghapus data!")

        // Hapus file gambar terkait
        data.imageUrl?.let { File(it).apply { if (exists()) delete() } }

        call.respond(DataResponse("success", "Berhasil menghapus data etnografi", null))
    }

    // Menampilkan file foto ke client
    suspend fun getImage(call: ApplicationCall) {
        val id = call.parameters["id"] ?: throw AppException(400, "ID tidak valid!")
        val data = ethnographyRepo.getById(id) ?: return call.respond(HttpStatusCode.NotFound)

        val file = data.imageUrl?.let { File(it) }
        if (file == null || !file.exists()) throw AppException(404, "Foto tidak tersedia")

        call.respondFile(file)
    }
}