package com.meowfia.app.engine

import com.meowfia.app.config.RoleResolutionConfig
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.util.RandomProvider

/**
 * Resolves all night actions in priority order.
 * Returns a populated ResolutionContext with all results.
 */
class NightResolver(private val random: RandomProvider) {

    fun resolve(gameState: GameState): ResolutionContext {
        val context = ResolutionContext(
            players = gameState.players,
            pool = gameState.pool,
            visitGraph = gameState.visitGraph,
            random = random
        )

        val useDandelion = gameState.activeFlowers.contains(RoleId.DANDELION)
        val sortedActors = if (useDandelion) {
            RoleResolutionConfig.getSeatOrder(gameState.players, gameState.dealerSeat)
        } else {
            RoleResolutionConfig.getResolutionOrder(gameState.players, gameState.dealerSeat)
        }

        for (actor in sortedActors) {
            val handler = RoleRegistry.get(actor.roleId)
            val target = gameState.visitGraph[actor.id]?.let { targetId ->
                gameState.players.find { it.id == targetId }
            }
            handler.resolve(actor, target, gameState, context)
        }

        return context
    }
}
