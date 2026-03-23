package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Visit a random player, lay an egg. Target is pre-resolved by GameCoordinator. */
class MosquitoHandler : RoleHandler {
    override val roleId = RoleId.MOSQUITO

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.Automatic("You visit a random player and lay an egg in their nest.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Mosquito) is hugged — action blocked.")
            return
        }
        if (target == null) return

        context.addEggs(target.id, 1)
        context.log("${actor.name} (Mosquito) visits ${target.name} (random) — egg laid.")
    }
}
