package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class TwinflowerHandler : FlowerHandler {
    override val roleId = RoleId.TWINFLOWER
    override val timing = FlowerTiming.MODIFIES_NIGHT
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "No limit on shared roles this round."
}
