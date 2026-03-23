package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

/**
 * Sunflower: During the day phase, one player may publicly reveal their role to gain 2 eggs.
 * In v1, the app announces this as a reminder. The reveal and egg gain are handled manually.
 */
class SunflowerHandler : FlowerHandler {
    override val roleId = RoleId.SUNFLOWER
    override val timing = FlowerTiming.DURING_DAY

    override fun activate(gameState: GameState): GameState {
        // Day-phase effect — handled as UI reminder, no state change here
        return gameState
    }

    override fun getDescription() =
        "One player may publicly reveal their role during the day phase to gain 2 eggs."
}
