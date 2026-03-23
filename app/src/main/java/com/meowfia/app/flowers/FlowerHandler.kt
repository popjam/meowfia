package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext

/** Interface for flower card effects. Each flower activates at a specific timing. */
interface FlowerHandler {
    val roleId: RoleId
    val timing: FlowerTiming
    fun activate(gameState: GameState): GameState
    fun getDescription(): String

    /** Apply pre-resolution effects (e.g. Wolfsbane egg bonus) into the ResolutionContext. */
    fun applyPreResolution(context: ResolutionContext, gameState: GameState) {}
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
