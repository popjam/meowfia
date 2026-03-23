package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class NightshadeHandler : FlowerHandler {
    override val roleId = RoleId.NIGHTSHADE
    override val timing = FlowerTiming.MODIFIES_NIGHT
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Next round has a Dusk phase before Night."
}
