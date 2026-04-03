package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player. Learn their role and lay an egg in their nest. If anyone visits you, you will be confused. */
class KookaburraHandler : RoleHandler {
    override val roleId = RoleId.KOOKABURRA

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. You'll learn their role and lay an egg in their nest. If anyone visits you, you'll be confused.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Kookaburra) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val targetRole = context.getCurrentRole(target.id)
        context.addInfo(actor.id, "${target.name} is a ${targetRole.displayName}.")
        context.addEggs(target.id, 1)
        context.log("${actor.name} (Kookaburra) visits ${target.name} — learns role (${targetRole.displayName}), egg laid in their nest.")

        if (context.getVisitorsOf(actor.id).isNotEmpty()) {
            context.applyEffect(actor.id, StatusEffect.CONFUSED)
            context.log("${actor.name} (Kookaburra) was visited — becomes confused.")
        }
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.OPPOSITE_TEAM

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
