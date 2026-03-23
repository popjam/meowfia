package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class BluebellHandler : FlowerHandler {
    override val roleId = RoleId.BLUEBELL
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "One CAW CAW gains an egg and ends the day."
}
