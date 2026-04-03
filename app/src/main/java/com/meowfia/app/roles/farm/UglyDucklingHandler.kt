package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** You appear as Meowfia to investigators. Visit a player and steal 1 egg from their nest. */
class UglyDucklingHandler : RoleHandler {
    override val roleId = RoleId.UGLY_DUCKLING

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to visit and steal 1 egg from their nest.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Ugly Duckling) is hugged — action blocked.")
            return
        }
        if (target == null) return

        context.removeEggs(target.id, 1)
        context.log("${actor.name} (Ugly Duckling) steals 1 egg from ${target.name}'s nest.")
    }

    override fun getApparentAlignment(player: Player): Alignment = Alignment.MEOWFIA

    override fun getTargetPreference(actor: Player) = TargetPreference.OPPOSITE_TEAM
}
