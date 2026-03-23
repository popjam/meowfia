package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

/** Interface for flower card effects. Each flower activates at a specific timing. */
interface FlowerHandler {
    val roleId: RoleId
    val timing: FlowerTiming
    fun activate(gameState: GameState): GameState
    fun getDescription(): String
}

enum class FlowerTiming {
    ON_REVEAL,
    MODIFIES_NIGHT,
    MODIFIES_DAWN,
    DURING_DAY,
    MODIFIES_VOTING,
    MODIFIES_SCORING,
    FULL_ROUND
}
