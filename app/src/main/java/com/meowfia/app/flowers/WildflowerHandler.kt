package com.meowfia.app.flowers

import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.engine.ResolutionContext

/** A random event happens during night — extra egg, role swap, Wink, or bonus eggs. */
class WildflowerHandler : FlowerHandler {
    override val roleId = RoleId.WILDFLOWER
    override val timing = FlowerTiming.MODIFIES_NIGHT

    override fun activate(gameState: GameState): GameState = gameState

    override fun getDescription() =
        "A random event happens during the night phase."

    override fun applyPreResolution(context: ResolutionContext, gameState: GameState) {
        val players = gameState.players
        if (players.isEmpty()) return

        when (context.random.nextInt(4)) {
            0 -> {
                // Event 1: Extra egg to a random player
                val recipient = players[context.random.nextInt(players.size)]
                context.addEggs(recipient.id, 1)
                context.log("Wildflower event: ${recipient.name} receives a bonus egg.")
            }
            1 -> {
                // Event 2: Two players swap roles
                if (players.size >= 2) {
                    val shuffled = context.random.shuffle(players)
                    val playerA = shuffled[0]
                    val playerB = shuffled[1]
                    context.swapRoles(playerA.id, playerB.id)
                    context.log("Wildflower event: ${playerA.name} and ${playerB.name} swap roles.")
                }
            }
            2 -> {
                // Event 3: A player gets a Wink
                val recipient = players[context.random.nextInt(players.size)]
                context.applyEffect(recipient.id, StatusEffect.HAS_WINK)
                context.log("Wildflower event: ${recipient.name} receives a Wink.")
            }
            3 -> {
                // Event 4: All players get +1 egg
                for (player in players) {
                    context.addEggs(player.id, 1)
                }
                context.log("Wildflower event: All players receive a bonus egg.")
            }
        }
    }
}
