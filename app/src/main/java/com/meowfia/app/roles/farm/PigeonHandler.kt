package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player, lay 1 egg in their nest. */
class PigeonHandler : RoleHandler {
    override val roleId = RoleId.PIGEON

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to visit and lay an egg in their nest.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Pigeon) is hugged — action blocked.")
            return
        }
        if (target == null) return

        context.addEggs(target.id, 1)
        context.log("${actor.name} (Pigeon) lays egg in ${target.name}'s nest.")
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.SAME_TEAM
}
