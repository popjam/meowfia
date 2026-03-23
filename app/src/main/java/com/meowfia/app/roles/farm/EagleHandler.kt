package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Gain eggs equal to the number of visitors your target received. */
class EagleHandler : RoleHandler {
    override val roleId = RoleId.EAGLE

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. You gain eggs equal to their visitor count.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Eagle) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val visitorCount = context.getVisitorsOf(target.id)
            .filter { it.id != actor.id } // Eagle doesn't count itself
            .size

        if (visitorCount > 0) {
            context.addEggs(actor.id, visitorCount)
            context.log("${actor.name} (Eagle) visits ${target.name} — $visitorCount visitor(s) → $visitorCount egg(s).")
        } else {
            context.log("${actor.name} (Eagle) visits ${target.name} — 0 visitors, no eggs.")
        }
    }
}
