package com.meowfia.app.bot

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.engine.GameCoordinator
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class BotIntegrationTest {

    @Before
    fun setup() {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
    }

    @Test
    fun full_round_with_all_bots() {
        val random = RandomProvider(42)
        val coordinator = GameCoordinator(random)
        val botNames = BotNames.pick(6, random)

        val pool = listOf(
            PoolCard(RoleId.PIGEON),
            PoolCard(RoleId.HOUSE_CAT),
            PoolCard(RoleId.HAWK)
        )

        coordinator.startNewRound(
            roundNumber = 1,
            poolCards = pool,
            playerNames = botNames,
            dealerSeat = 0
        )

        val assignments = coordinator.assignRoles()
        assertThat(assignments).hasSize(6)

        // Flag all as bots
        val updatedPlayers = coordinator.state.players.map { it.copy(isBot = true) }
        coordinator.updatePlayers(updatedPlayers)

        // Each bot submits a night action
        for (player in coordinator.state.players) {
            val action = BotBrain.chooseNightAction(player, coordinator.state.players, random)
            coordinator.submitNightAction(player.id, action)
        }
        assertThat(coordinator.allNightActionsSubmitted()).isTrue()

        // Resolve night
        coordinator.resolveNight()

        // Generate dawn reports
        val dawnReports = (0 until 6).map { coordinator.getDawnReport(it) }
        assertThat(dawnReports).hasSize(6)

        // Generate bot claims
        val state = coordinator.state
        val claims = state.players.map { bot ->
            BotClaimGenerator.generateClaim(
                bot = bot,
                allPlayers = state.players,
                dawnReport = state.dawnReports[bot.id]!!,
                visitGraph = state.visitGraph,
                pool = pool.map { it.roleId }.filter { it.isFarmAnimal },
                random = random
            )
        }
        assertThat(claims).hasSize(6)

        // All claims should have valid roles
        for (claim in claims) {
            assertThat(claim.claimedRole).isNotNull()
            assertThat(claim.botName).isNotEmpty()
        }

        // Eggsecute a player
        coordinator.recordEggsecution(0)
        val winningTeam = coordinator.getWinningTeam()
        assertThat(winningTeam).isNotNull()
    }
}
