package com.meowfia.app.roles.meowfia

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/**
 * Start with a Wink. Visit a player and learn their role.
 *
 * TODO: The starting Wink (StatusEffect.HAS_WINK) should be applied during role assignment.
 * The night action only handles the intel ability.
 */
class MouserHandler : RoleHandler {
    override val roleId = RoleId.MOUSER

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player. You will learn their role.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Mouser) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val targetRole = context.getCurrentRole(target.id)
        context.addInfo(actor.id, "${target.name} is a ${targetRole.displayName}.")
        context.log("${actor.name} (Mouser) visits ${target.name} — learns role (${targetRole.displayName}).")
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.OPPOSITE_TEAM

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
