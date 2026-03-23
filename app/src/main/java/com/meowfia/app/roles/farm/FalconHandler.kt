package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Lay an egg in the nest of whoever your target visited. */
class FalconHandler : RoleHandler {
    override val roleId = RoleId.FALCON

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. You'll lay an egg in the nest of whoever they visited.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Falcon) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val targetsTarget = context.getVisitTargetOf(target.id)
        if (targetsTarget != null) {
            context.addEggs(targetsTarget.id, 1)
            context.log("${actor.name} (Falcon) visits ${target.name}, who visited ${targetsTarget.name} — egg laid in ${targetsTarget.name}'s nest.")
        } else {
            context.log("${actor.name} (Falcon) visits ${target.name} — they visited nobody. No egg.")
        }
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.ACTIVE_VISITORS
}
