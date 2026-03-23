package com.meowfia.app.roles.meowfia

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Visit a player. Learn their role and who they visited. */
class HouseCatHandler : RoleHandler {
    override val roleId = RoleId.HOUSE_CAT

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to visit. You'll learn their role and who they visited.")

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

        // Intel only — no stealing in v6
        val visitTarget = context.getVisitTargetOf(target.id)
        val visitInfo = if (visitTarget != null) "visited ${visitTarget.name}" else "visited nobody"
        context.addInfo(actor.id, "${target.name} is a ${target.roleId.displayName} and $visitInfo.")
        context.log("${actor.name} (House Cat) visits ${target.name} — learns role and visit target.")
    }

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
