package com.meowfia.app.engine

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GamePhase
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class GameCoordinatorTest {

    @Before
    fun setup() {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
    }

    private fun createCoordinator(): GameCoordinator {
        return GameCoordinator(RandomProvider(42))
    }

    private val testPool = listOf(
        PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT), PoolCard(RoleId.HAWK)
    )

    private val testNames = listOf("Alice", "Bob", "Charlie", "Diana")

    @Test
    fun start_round_sets_pool_setup_phase() {
        val coord = createCoordinator()
        coord.startNewRound(1, testPool, testNames, 0)
        assertThat(coord.getCurrentPhase()).isEqualTo(GamePhase.POOL_SETUP)
    }

    @Test
    fun assign_roles_sets_role_reveal_phase() {
        val coord = createCoordinator()
        coord.startNewRound(1, testPool, testNames, 0)
        coord.assignRoles()
        assertThat(coord.getCurrentPhase()).isEqualTo(GamePhase.ROLE_REVEAL)
    }

    @Test
    fun submit_all_night_actions_marks_submitted() {
        val coord = createCoordinator()
        coord.startNewRound(1, testPool, testNames, 0)
        coord.assignRoles()

        for (i in testNames.indices) {
            coord.submitNightAction(i, NightAction.VisitPlayer((i + 1) % testNames.size))
        }
        assertThat(coord.allNightActionsSubmitted()).isTrue()
    }

    @Test
    fun resolve_night_transitions_to_dawn() {
        val coord = createCoordinator()
        coord.startNewRound(1, testPool, testNames, 0)
        coord.assignRoles()

        for (i in testNames.indices) {
            val action = if (coord.state.players[i].roleId == RoleId.BLACK_SWAN) {
                NightAction.VisitSelf
            } else if (coord.state.players[i].roleId == RoleId.TURKEY ||
                coord.state.players[i].roleId == RoleId.MOSQUITO ||
                coord.state.players[i].roleId == RoleId.TIT
            ) {
                NightAction.VisitRandom
            } else {
                NightAction.VisitPlayer((i + 1) % testNames.size)
            }
            coord.submitNightAction(i, action)
        }
        coord.resolveNight()
        assertThat(coord.getCurrentPhase()).isEqualTo(GamePhase.DAWN)
    }

    @Test
    fun caw_caw_triggers_at_three() {
        val coord = createCoordinator()
        coord.startNewRound(1, testPool, testNames, 0)
        coord.assignRoles()
        coord.startDay()

        assertThat(coord.isCawCawTriggered()).isFalse()
        coord.recordCawCaw()
        coord.recordCawCaw()
        assertThat(coord.isCawCawTriggered()).isFalse()
        coord.recordCawCaw()
        assertThat(coord.isCawCawTriggered()).isTrue()
    }

    @Test
    fun eggsecution_determines_winning_team() {
        val coord = createCoordinator()
        coord.startNewRound(1, testPool, testNames, 0)
        val assignments = coord.assignRoles(
            forcedAlignments = mapOf(0 to Alignment.MEOWFIA, 1 to Alignment.FARM, 2 to Alignment.FARM, 3 to Alignment.FARM)
        )

        coord.recordEggsecution(0) // eliminate Meowfia player
        assertThat(coord.getWinningTeam()).isEqualTo(Alignment.FARM)
    }

    @Test
    fun player_count_correct() {
        val coord = createCoordinator()
        coord.startNewRound(1, testPool, testNames, 0)
        assertThat(coord.getPlayerCount()).isEqualTo(4)
    }
}
