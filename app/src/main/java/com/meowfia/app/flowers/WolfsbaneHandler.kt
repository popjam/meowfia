package com.meowfia.app.flowers

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId

/**
 * Wolfsbane: On pool reveal, all Meowfia players gain 1 extra egg.
 * Applied during pool setup, before night phase begins.
 */
class WolfsbaneHandler : FlowerHandler {
    override val roleId = RoleId.WOLFSBANE
    override val timing = FlowerTiming.ON_REVEAL

    override fun activate(gameState: GameState): GameState {
        val updatedPlayers = gameState.players.map { player ->
            if (player.alignment == Alignment.MEOWFIA) {
                player.copy(nestEggCount = (player.nestEggCount + 1).coerceAtMost(5))
            } else {
                player
            }
        }
        return gameState.copy(players = updatedPlayers)
    }

    override fun getDescription() =
        "All Meowfia players gain 1 extra egg this round."
}
