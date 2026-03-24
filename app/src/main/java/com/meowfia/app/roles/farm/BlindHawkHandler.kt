package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player. If the player THEY visited is Meowfia, lay an egg in your nest. */
class BlindHawkHandler : RoleHandler {
    override val roleId = RoleId.BLIND_HAWK

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to investigate. If the player they visited is Meowfia, you gain an egg.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Blind Hawk) is hugged — action blocked.")
            return
        }
        if (target == null) return

        val visitTarget = context.getVisitTargetOf(target.id)
        if (visitTarget != null) {
            val alignment = context.getCurrentAlignment(visitTarget.id)
            if (alignment == Alignment.MEOWFIA) {
                context.addEggs(actor.id, 1)
                context.log("${actor.name} (Blind Hawk) investigates ${target.name}'s target ${visitTarget.name} — MEOWFIA. Egg gained.")
            } else {
                context.log("${actor.name} (Blind Hawk) investigates ${target.name}'s target ${visitTarget.name} — FARM. No egg.")
            }
        } else {
            context.log("${actor.name} (Blind Hawk) investigates ${target.name} — target visited nobody. No egg.")
        }
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.ACTIVE_VISITORS
    override fun getSelfEggRange() = 0..1

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return emptyList()
    }
}
