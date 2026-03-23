package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

/**
 * Sunflower: A player can scan this card and spend one egg to secretly learn
 * if a chosen player is a certain animal or not. Card prompt handled in UI.
 */
class SunflowerHandler : FlowerHandler {
    override val roleId = RoleId.SUNFLOWER
    override val timing = FlowerTiming.DURING_DAY
    override fun activate(gameState: GameState): GameState = gameState
    override fun getDescription() =
        "A player can scan this card and spend one egg to secretly learn if a chosen player is a certain animal or not."
}
