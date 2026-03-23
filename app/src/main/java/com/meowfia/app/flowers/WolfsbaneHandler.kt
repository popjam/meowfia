package com.meowfia.app.flowers

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext

/**
 * Wolfsbane: On pool reveal, all Meowfia players gain 1 extra egg.
 * Applied as a pre-resolution egg delta before night resolution runs.
 */
class WolfsbaneHandler : FlowerHandler {
    override val roleId = RoleId.WOLFSBANE
    override val timing = FlowerTiming.ON_REVEAL

    override fun activate(gameState: GameState): GameState {
        // No longer modifies Player state directly; effect applied via applyPreResolution
        return gameState
    }

    override fun applyPreResolution(context: ResolutionContext, gameState: GameState) {
        for (player in gameState.players) {
            if (player.alignment == Alignment.MEOWFIA) {
                context.addEggs(player.id, 1)
            }
        }
    }

    override fun getDescription() =
        "All Meowfia players gain 1 extra egg this round."
}
