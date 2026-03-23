package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Visit yourself — if you're still a Black Swan (role hasn't been swapped), gain an egg. */
class BlackSwanHandler : RoleHandler {
    override val roleId = RoleId.BLACK_SWAN

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.SelfVisit("You visit yourself. If you're still a Black Swan, you gain an egg.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Black Swan) is hugged — action blocked.")
            return
        }

        if (actor.roleId == RoleId.BLACK_SWAN) {
            context.addEggs(actor.id, 1)
            context.log("${actor.name} (Black Swan) visits self — still a Black Swan. Egg gained.")
        } else {
            context.log("${actor.name} (Black Swan) visits self — role has been swapped. No egg.")
        }
    }
}
