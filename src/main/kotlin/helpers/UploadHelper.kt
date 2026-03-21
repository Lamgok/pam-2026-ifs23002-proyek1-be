package org.delcom.helpers

import io.ktor.http.ContentType
import io.ktor.http.content.PartData
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.delcom.data.AppException
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private val allowedImageExtensions = setOf("jpg", "jpeg", "png", "webp")
private val allowedImageContentTypes = setOf(
    ContentType.Image.JPEG.toString(),
    ContentType.Image.PNG.toString(),
    "image/webp"
)

suspend fun saveImageUpload(
    part: PartData.FileItem,
    targetDirectory: String,
    maxBytes: Long = 5L * 1024L * 1024L
): String {
    val extension = part.originalFileName
        ?.substringAfterLast('.', "")
        ?.lowercase()
        ?.takeIf { it.isNotBlank() }
        ?: throw AppException(400, "Ekstensi file gambar tidak valid!")

    if (extension !in allowedImageExtensions) {
        throw AppException(400, "Format file gambar tidak didukung!")
    }

    val contentType = part.contentType?.toString()
        ?: throw AppException(400, "Tipe konten file tidak valid!")
    if (contentType !in allowedImageContentTypes) {
        throw AppException(400, "Tipe file gambar tidak didukung!")
    }

    val fileName = "${UUID.randomUUID()}.$extension"
    val destination = File(targetDirectory, fileName)

    withContext(Dispatchers.IO) {
        destination.parentFile.mkdirs()
        FileOutputStream(destination).use { output ->
            copyChannelWithLimit(part.provider(), output, maxBytes)
        }
    }

    return destination.path.replace("\\", "/")
}

private suspend fun copyChannelWithLimit(
    channel: ByteReadChannel,
    output: FileOutputStream,
    maxBytes: Long
) {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L

    while (!channel.isClosedForRead) {
        val read = channel.readAvailable(buffer, 0, buffer.size)
        if (read <= 0) {
            continue
        }

        totalBytes += read
        if (totalBytes > maxBytes) {
            throw AppException(400, "Ukuran file gambar maksimal 5 MB!")
        }

        output.write(buffer, 0, read)
    }
}
