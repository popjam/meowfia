package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class MoonflowerHandler : FlowerHandler {
    override val roleId = RoleId.MOONFLOWER
    override val timing = FlowerTiming.MODIFIES_NIGHT
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Two consecutive night phases this round."
}
