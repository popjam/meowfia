package com.meowfia.app.roles.meowfia

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player and hug them. Any players that visit you will be hugged. You cannot be hugged. */
class FlooferHandler : RoleHandler {
    override val roleId = RoleId.FLOOFER

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to hug. Visitors to you will also be hugged. You cannot be hugged.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        // Floofer is immune to hugs — always acts regardless of hug status
        if (target == null) return

        context.applyEffect(target.id, StatusEffect.HUGGED)
        context.log("${actor.name} (Floofer) visits ${target.name} — target hugged.")

        for (visitor in context.getVisitorsOf(actor.id)) {
            context.applyEffect(visitor.id, StatusEffect.HUGGED)
            context.log("${visitor.name} visited ${actor.name} (Floofer) — becomes hugged.")
        }
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.OPPOSITE_TEAM
}
