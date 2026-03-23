package com.meowfia.app.roles.meowfia

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Visit a player, steal an egg, learn their role and who they visited. */
class HouseCatHandler : RoleHandler {
    override val roleId = RoleId.HOUSE_CAT

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to visit. You'll steal an egg and learn their role.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (House Cat) is hugged — action blocked.")
            return
        }
        if (target == null) return

        // Steal: if target has eggs, remove 1 from target, add 1 to self
        val targetNest = context.getNestCount(target.id)
        if (targetNest > 0) {
            context.removeEggs(target.id, 1)
            context.addEggs(actor.id, 1)
            context.log("${actor.name} (House Cat) steals egg from ${target.name}.")
        } else {
            context.log("${actor.name} (House Cat) visits ${target.name} — nest is empty, nothing to steal.")
        }

        // Intel: learn target's role and who they visited
        val visitTarget = context.getVisitTargetOf(target.id)
        val visitInfo = if (visitTarget != null) "visited ${visitTarget.name}" else "visited nobody"
        context.addInfo(actor.id, "${target.name} is a ${target.roleId.displayName} and $visitInfo.")
    }

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
