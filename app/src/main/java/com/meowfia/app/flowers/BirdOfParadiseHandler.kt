package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

class BirdOfParadiseHandler : FlowerHandler {
    override val roleId = RoleId.BIRD_OF_PARADISE
    override val timing = FlowerTiming.ON_REVEAL
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "Adds an extra bot player to the game this round."
}
