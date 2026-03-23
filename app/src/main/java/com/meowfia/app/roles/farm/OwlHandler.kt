package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Learn which animals visited your target; if none visited, lay an egg. */
class OwlHandler : RoleHandler {
    override val roleId = RoleId.OWL

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. You'll learn which animals visited them.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Owl) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val visitors = context.getVisitorsOf(target.id)
            .filter { it.id != actor.id } // Owl doesn't count itself

        if (visitors.isEmpty()) {
            context.addEggs(target.id, 1)
            context.addInfo(actor.id, "No animals visited ${target.name}. Egg laid in their nest.")
            context.log("${actor.name} (Owl) checks ${target.name} — no visitors. Egg laid.")
        } else {
            val animalNames = visitors.map { it.roleId.displayName }
            context.addInfo(actor.id, "Animals that visited ${target.name}: ${animalNames.joinToString(", ")}.")
            context.log("${actor.name} (Owl) checks ${target.name} — visitors: ${animalNames.joinToString(", ")}.")
        }
    }

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
