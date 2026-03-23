package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Stay home — lay an egg for each player who visits you. */
class TurkeyHandler : RoleHandler {
    override val roleId = RoleId.TURKEY

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.Automatic("You stay home. You'll lay an egg for each player who visits you.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Turkey) is hugged — action blocked.")
            return
        }

        val visitors = context.getVisitorsOf(actor.id)

        if (visitors.isNotEmpty()) {
            for (visitor in visitors) {
                context.addEggs(visitor.id, 1)
            }
            context.log("${actor.name} (Turkey) — ${visitors.size} visitor(s): ${visitors.joinToString { it.name }}. ${visitors.size} egg(s) laid.")
        } else {
            context.log("${actor.name} (Turkey) — 0 visitors, no eggs.")
        }
    }
}
