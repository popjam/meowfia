package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

/**
 * Dandelion: Night actions resolve in seat order instead of tier order.
 * The NightResolver checks for DANDELION in activeFlowers and switches to seat order.
 */
class DandelionHandler : FlowerHandler {
    override val roleId = RoleId.DANDELION
    override val timing = FlowerTiming.MODIFIES_NIGHT

    override fun activate(gameState: GameState): GameState {
        // Effect is checked by NightResolver via activeFlowers list
        return gameState
    }

    override fun getDescription() =
        "Night actions resolve in seat order instead of tier order this round."
}
