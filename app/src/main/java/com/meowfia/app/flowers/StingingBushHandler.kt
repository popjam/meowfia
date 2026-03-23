package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class StingingBushHandler : FlowerHandler {
    override val roleId = RoleId.STINGING_BUSH
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "First player to touch another assassinates them."
}
