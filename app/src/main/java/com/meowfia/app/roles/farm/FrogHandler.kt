package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Visit a player and swap animals (roles) with them. You learn your new animal at dawn. */
class FrogHandler : RoleHandler {
    override val roleId = RoleId.FROG

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to swap animals with.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Frog) is hugged — action blocked.")
            return
        }

        if (target == null) {
            context.log("${actor.name} (Frog) has no target — no swap.")
            return
        }

        val actorOldRole = context.getCurrentRole(actor.id)
        val targetRole = context.getCurrentRole(target.id)

        context.swapRoles(actor.id, target.id)

        context.addInfo(actor.id, "You swapped with ${target.name}. You are now a ${targetRole.displayName}.")
        context.log("${actor.name} (Frog) swapped roles with ${target.name}. ${actorOldRole.displayName} → ${targetRole.displayName}, ${target.name} → ${actorOldRole.displayName}.")
    }

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
