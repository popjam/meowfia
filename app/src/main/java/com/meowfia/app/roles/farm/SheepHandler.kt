package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Visit a player. You become their alignment. You do NOT learn your new alignment. */
class SheepHandler : RoleHandler {
    override val roleId = RoleId.SHEEP

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. You will become their alignment.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Sheep) is hugged — action blocked.")
            return
        }

        if (target == null) {
            context.log("${actor.name} (Sheep) has no target — no alignment change.")
            return
        }

        val targetAlignment = context.getCurrentAlignment(target.id)
        context.setAlignment(actor.id, targetAlignment)
        context.log("${actor.name} (Sheep) visited ${target.name} — alignment now ${targetAlignment.displayName}.")
    }

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return emptyList()
    }
}
