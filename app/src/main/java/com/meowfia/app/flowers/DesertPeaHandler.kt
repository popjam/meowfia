package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class DesertPeaHandler : FlowerHandler {
    override val roleId = RoleId.DESERT_PEA
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "A player may guess all animals for exclusive victory."
}
