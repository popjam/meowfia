package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class TumbleweedHandler : FlowerHandler {
    override val roleId = RoleId.TUMBLEWEED
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Players walk freely — no assigned seats."
}
