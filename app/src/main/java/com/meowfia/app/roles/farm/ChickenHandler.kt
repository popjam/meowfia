package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player and lay 2 eggs. Lose if anyone throws a single egg at you during voting. */
class ChickenHandler : RoleHandler {
    override val roleId = RoleId.CHICKEN

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to visit and lay 2 eggs. Careful — you lose if anyone throws a single egg at you!")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Chicken) is hugged — action blocked.")
            return
        }
        if (target == null) return

        context.addEggs(target.id, 2)
        context.log("${actor.name} (Chicken) lays 2 eggs in ${target.name}'s nest.")
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.SAME_TEAM
}
