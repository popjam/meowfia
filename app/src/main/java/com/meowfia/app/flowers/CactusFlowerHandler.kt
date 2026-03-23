package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class CactusFlowerHandler : FlowerHandler {
    override val roleId = RoleId.CACTUS_FLOWER
    override val timing = FlowerTiming.ON_REVEAL
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "The app reveals how many Meowfia there are this round."
}
