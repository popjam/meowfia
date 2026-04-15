package com.meowfia.app.data.model

import android.graphics.Bitmap

/**
 * Pre-configuration data for a player slot, set during pool setup.
 * A null [name] means the player hasn't been customized and needs full registration.
 */
data class PreConfiguredPlayer(
    val name: String? = null,
    val profileBitmap: Bitmap? = null,
    val isBot: Boolean = false
)
