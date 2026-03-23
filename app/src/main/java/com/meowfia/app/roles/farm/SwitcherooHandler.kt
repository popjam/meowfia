package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player. They swap animals with whoever they visited. Neither player is informed. */
class SwitcherooHandler : RoleHandler {
    override val roleId = RoleId.SWITCHEROO

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. They will swap animals with whoever they visited.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Switcheroo) is hugged — action blocked.")
            return
        }

        if (target == null) {
            context.log("${actor.name} (Switcheroo) has no target — nothing happens.")
            return
        }

        val visitTarget = context.getVisitTargetOf(target.id)
        if (visitTarget == null) {
            context.log("${actor.name} (Switcheroo) targeted ${target.name}, but they didn't visit anyone — no swap.")
            return
        }

        val targetRole = context.getCurrentRole(target.id)
        val visitTargetRole = context.getCurrentRole(visitTarget.id)

        context.swapRoles(target.id, visitTarget.id)

        context.log("${actor.name} (Switcheroo) caused ${target.name} and ${visitTarget.name} to swap roles. ${target.name} → ${visitTargetRole.displayName}, ${visitTarget.name} → ${targetRole.displayName}.")
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.ACTIVE_VISITORS

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return emptyList()
    }
}
