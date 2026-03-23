package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

/**
 * Wolfsbane: Meowfia assignment chance is 1 in 2 instead of 1 in 3 this round.
 * The actual effect is handled by RoleAssigner which checks for WOLFSBANE in activeFlowers.
 */
class WolfsbaneHandler : FlowerHandler {
    override val roleId = RoleId.WOLFSBANE
    override val timing = FlowerTiming.ON_REVEAL
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Meowfia assignment chance is 1 in 2 instead of 1 in 3 this round."
}
