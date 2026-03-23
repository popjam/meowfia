package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class GoldenWattleHandler : FlowerHandler {
    override val roleId = RoleId.GOLDEN_WATTLE
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Two players may shake hands to bond for +3/-3 points."
}
