package com.meowfia.app.roles.farm

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.RoleHandler

/** Learn an animal. Three random players each receive an egg — one of them is that animal. */
class MagpieHandler : RoleHandler {
    override val roleId = RoleId.MAGPIE

    override fun getNightPrompt(player: Player, allPlayers: List<Player>) =
        NightPrompt.Automatic("The Magpie learns an animal at night. Three random players receive eggs — one is that animal.")

    override fun getValidTargets(player: Player, allPlayers: List<Player>): List<Player> =
        emptyList() // Magpie doesn't visit — targets are chosen internally

    override fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    ) {
        if (context.isHugged(actor.id)) {
            context.log("${actor.name} (Magpie) is hugged — action blocked.")
            return
        }

        // Pick a random non-buffer role from among other players
        val otherPlayers = gameState.players.filter { it.id != actor.id }
        val nonBufferPlayers = otherPlayers.filter { !context.getCurrentRole(it.id).isBuffer }

        if (nonBufferPlayers.isEmpty()) {
            context.log("${actor.name} (Magpie) — no non-buffer roles to pick from.")
            return
        }

        // Pick one random non-buffer player to be the "target role" player
        val chosenPlayer = nonBufferPlayers[context.random.nextInt(nonBufferPlayers.size)]
        val pickedRole = context.getCurrentRole(chosenPlayer.id)

        // Build the three egg recipients: the chosen player + 2 random others
        val remainingPlayers = otherPlayers.filter { it.id != chosenPlayer.id }
        val shuffled = context.random.shuffle(remainingPlayers)
        val extraRecipients = shuffled.take(2.coerceAtMost(shuffled.size))
        val allRecipients = (listOf(chosenPlayer) + extraRecipients)
        val shuffledRecipients = context.random.shuffle(allRecipients)

        for (recipient in shuffledRecipients) {
            context.addEggs(recipient.id, 1)
        }

        val recipientNames = shuffledRecipients.joinToString(", ") { it.name }
        context.addInfo(actor.id, "The animal is ${pickedRole.displayName}. Three players received eggs — one of them is that animal: $recipientNames.")
        context.log("${actor.name} (Magpie) learns ${pickedRole.displayName}. Eggs given to: $recipientNames.")
    }

    override fun getDawnInfo(player: Player, context: ResolutionContext): List<String> {
        return context.getInfoFor(player.id)
    }
}
