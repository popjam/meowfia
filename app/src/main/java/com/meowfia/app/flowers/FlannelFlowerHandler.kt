package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class FlannelFlowerHandler : FlowerHandler {
    override val roleId = RoleId.FLANNEL_FLOWER
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Players must keep eyes shut during the day phase."
}
