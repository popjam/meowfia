package com.meowfia.app.qr

import com.meowfia.app.data.model.RoleId

/**
 * Parses QR code content in the format `MEOWFIA:V1:<ROLE_ID>`.
 * Returns the corresponding [RoleId] or null if the format is invalid.
 */
class QrScanner {

    fun parseQrContent(rawValue: String): RoleId? {
        val parts = rawValue.split(":")
        if (parts.size != 3 || parts[0] != "MEOWFIA" || parts[1] != "V1") return null
        return try {
            RoleId.valueOf(parts[2])
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
