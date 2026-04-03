package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player and confuse them. Any players that visit you will become confused. */
class KoalaHandler : RoleHandler {
    override val roleId = RoleId.KOALA

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to confuse. Any players that visit you will also become confused.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Koala) is hugged — action blocked.")
            return
        }
        if (target == null) return

        context.applyEffect(target.id, StatusEffect.CONFUSED)
        context.log("${actor.name} (Koala) visits ${target.name} — target confused.")

        for (visitor in context.getVisitorsOf(actor.id)) {
            context.applyEffect(visitor.id, StatusEffect.CONFUSED)
            context.log("${visitor.name} visited ${actor.name} (Koala) — becomes confused.")
        }
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.OPPOSITE_TEAM
}
