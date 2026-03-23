package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class BanksiaHandler : FlowerHandler {
    override val roleId = RoleId.BANKSIA
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Communication limited to whispering to neighbours."
}
