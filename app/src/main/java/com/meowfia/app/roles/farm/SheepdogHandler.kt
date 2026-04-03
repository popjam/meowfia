package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler
import com.meowfia.app.roles.TargetPreference

/** Visit a player and hug them. If there are any Sheep in the game, learn who they are. */
class SheepdogHandler : RoleHandler {
    override val roleId = RoleId.SHEEPDOG

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.PickPlayer("Choose a player to hug. If any Sheep are in the game, you'll learn who they are.")

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Sheepdog) is hugged — action blocked.")
            return
        }
        if (target == null) return

        context.applyEffect(target.id, StatusEffect.HUGGED)
        context.log("${actor.name} (Sheepdog) visits ${target.name} — target hugged.")

        val sheepPlayers = gameState.players.filter {
            context.getCurrentRole(it.id) == RoleId.SHEEP
        }
        if (sheepPlayers.isNotEmpty()) {
            val sheepNames = sheepPlayers.joinToString(", ") { it.name }
            context.addInfo(actor.id, "Sheep: $sheepNames.")
            context.log("${actor.name} (Sheepdog) detects Sheep: $sheepNames.")
        } else {
            context.log("${actor.name} (Sheepdog) — no Sheep in the game.")
        }
    }

    override fun getTargetPreference(actor: Player) = TargetPreference.OPPOSITE_TEAM

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
