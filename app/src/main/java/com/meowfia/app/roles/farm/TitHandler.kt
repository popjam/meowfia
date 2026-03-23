package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Visit a random Meowfia player and lay an egg. Target is pre-resolved by GameCoordinator. */
class TitHandler : RoleHandler {
    override val roleId = RoleId.TIT

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.Automatic("You visit a random Meowfia player and lay an egg in their nest.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Tit) is hugged — action blocked.")
            return
        }
        if (target == null) {
            context.log("${actor.name} (Tit) — no Meowfia players to visit. Nothing happens.")
            return
        }

        context.addEggs(target.id, 1)
        context.log("${actor.name} (Tit) visits ${target.name} (Meowfia) — egg laid.")
    }
}
