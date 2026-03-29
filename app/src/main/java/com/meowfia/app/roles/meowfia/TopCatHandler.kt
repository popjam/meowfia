package com.meowfia.app.roles.meowfia

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player. They switch alignments. */
class TopCatHandler : RoleHandler {
    override val roleId = RoleId.TOP_CAT

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. They will switch alignments.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Top Cat) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val currentAlignment = context.getCurrentAlignment(target.id)
        val flipped = when (currentAlignment) {
            Alignment.FARM -> Alignment.MEOWFIA
            Alignment.MEOWFIA -> Alignment.FARM
        }
        context.setAlignment(target.id, flipped)
        context.log("${actor.name} (Top Cat) visits ${target.name} — alignment flipped from ${currentAlignment.displayName} to ${flipped.displayName}.")
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.OPPOSITE_TEAM
}
