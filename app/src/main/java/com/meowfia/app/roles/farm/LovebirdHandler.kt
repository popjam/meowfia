package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player. If they are the same alignment as who they visited, lay an egg in your nest. */
class LovebirdHandler : RoleHandler {
    override val roleId = RoleId.LOVEBIRD

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. If they share an alignment with whoever they visited, you gain an egg.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Lovebird) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val visitTarget = context.getVisitTargetOf(target.id)
        if (visitTarget != null) {
            val targetAlignment = context.getCurrentAlignment(target.id)
            val visitTargetAlignment = context.getCurrentAlignment(visitTarget.id)
            if (targetAlignment == visitTargetAlignment) {
                context.addEggs(actor.id, 1)
                context.log("${actor.name} (Lovebird) checks ${target.name} and ${visitTarget.name} — same alignment. Egg gained.")
            } else {
                context.log("${actor.name} (Lovebird) checks ${target.name} and ${visitTarget.name} — different alignment. No egg.")
            }
        } else {
            context.log("${actor.name} (Lovebird) checks ${target.name} — target visited nobody. No egg.")
        }
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.ACTIVE_VISITORS
    override fun getSelfEggRange() = 0..1

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return emptyList()
    }
}
