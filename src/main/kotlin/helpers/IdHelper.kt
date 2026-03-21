package org.delcom.helpers

import org.delcom.data.AppException
import java.util.UUID

fun parseUuidOrThrow(rawId: String, fieldName: String = "ID"): UUID = try {
    UUID.fromString(rawId)
} catch (_: IllegalArgumentException) {
    throw AppException(400, "$fieldName tidak valid!")
}
