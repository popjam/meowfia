package com.meowfia.app.engine

import android.graphics.Bitmap
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.util.RandomProvider

/**
 * Singleton holding the current game's [GameCoordinator].
 * ViewModels access this to interact with the game engine.
 */
object GameSession {
    lateinit var coordinator: GameCoordinator
        private set

    var isInitialized = false
        private set

    /** Player profile drawings, keyed by player ID. Cosmetic — not part of engine state. */
    val profileImages = mutableMapOf<Int, Bitmap>()

    fun startNewGame(seed: Long? = null) {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
        coordinator = GameCoordinator(
            random = RandomProvider(seed ?: System.currentTimeMillis())
        )
        profileImages.clear()
        isInitialized = true
    }
}
