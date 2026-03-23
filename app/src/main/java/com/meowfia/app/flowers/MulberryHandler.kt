package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class MulberryHandler : FlowerHandler {
    override val roleId = RoleId.MULBERRY
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Day timer starts at 30 seconds. Spend an egg for 30 more."
}
