package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class PitcherPlantHandler : FlowerHandler {
    override val roleId = RoleId.PITCHER_PLANT
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "The app announces who visited whom at the start of the day."
}
