package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Investigate a player — if Meowfia, gain an egg. */
class HawkHandler : RoleHandler {
    override val roleId = RoleId.HAWK

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to investigate. If they're Meowfia, you gain an egg.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Hawk) is hugged — action blocked.")
            return
        }
        if (target == null) return

        if (target.alignment == Alignment.MEOWFIA) {
            context.addEggs(actor.id, 1)
            context.addInfo(actor.id, "${target.name} appears MEOWFIA. Egg gained.")
            context.log("${actor.name} (Hawk) investigates ${target.name} — MEOWFIA. Egg gained.")
        } else {
            context.addInfo(actor.id, "${target.name} appears FARM.")
            context.log("${actor.name} (Hawk) investigates ${target.name} — appears FARM.")
        }
    }

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
