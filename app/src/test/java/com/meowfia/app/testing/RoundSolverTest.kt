package com.meowfia.app.testing

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.testing.sim.ClaimData
import com.meowfia.app.testing.sim.RoundSolver
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class RoundSolverTest {

    @Before
    fun setup() {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
    }

    private fun report(id: Int, delta: Int = 0, info: List<String> = emptyList()) =
        DawnReport(playerId = id, reportedEggDelta = delta, actualEggDelta = delta, additionalInfo = info)

    @Test
    fun duplicate_non_buffer_role_produces_reasons() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.HAWK, 1, 1),
            1 to ClaimData(1, RoleId.HAWK, 2, 0),  // duplicate Hawk
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.HAWK),
            PlayerAssignment(1, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        assertThat(result.reasons.any { it.contains("Hawk") }).isTrue()
    }

    @Test
    fun impossible_egg_delta_eliminates_farm_worlds() {
        // Player 0 claims +3 eggs as Pigeon — impossible (Pigeon lays in target's nest, not own).
        // Only Farm-minority worlds are checked (max 1 Meowfia for 4 players).
        // Worlds where player 0 is the sole Meowfia should still be evaluated.
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 3),
            1 to ClaimData(1, RoleId.HAWK, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(1, Alignment.FARM, RoleId.HAWK),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        // Only size-0 and size-1 Meowfia subsets are checked (Farm-majority only)
        // 4 players → C(4,0) + C(4,1) = 5 total candidates
        assertThat(result.totalCandidates).isEqualTo(5)
        // Not all candidates should be consistent (the impossible +3 eliminates some)
        assertThat(result.consistentWorlds).isLessThan(result.totalCandidates)
    }

    @Test
    fun consistent_claims_produce_worlds() {
        // 6 players so we get more room for Farm-minority worlds (max 2 Meowfia).
        // Simple claims: everyone claims Pigeon visiting the next player.
        // Egg deltas: each player gets visited by one Pigeon = +1 delta.
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 1),
            1 to ClaimData(1, RoleId.PIGEON, 2, 1),
            2 to ClaimData(2, RoleId.PIGEON, 3, 1),
            3 to ClaimData(3, RoleId.PIGEON, 4, 1),
            4 to ClaimData(4, RoleId.PIGEON, 5, 1),
            5 to ClaimData(5, RoleId.PIGEON, 0, 1)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(4, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(5, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..5).map { report(it, delta = 1) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        // With buffer-only pool and all claiming Pigeon, multiple worlds should work
        assertThat(result.consistentWorlds).isGreaterThan(0)
    }

    @Test
    fun enumeration_counts_all_subsets() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 0),
            1 to ClaimData(1, RoleId.PIGEON, 0, 0),
            2 to ClaimData(2, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..2).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        // 3 players, only Farm-majority worlds (size 0 and 1): C(3,0) + C(3,1) = 4
        assertThat(result.totalCandidates).isEqualTo(4)
    }

    @Test
    fun owl_conflict_detected() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.OWL)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.OWL, 2, 0),
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),  // claims visiting player 2
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.OWL),
            PlayerAssignment(1, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        // Owl says nobody visited player 2, but player 1 claims they did
        val reports = listOf(
            report(0, info = listOf("No animals visited player 2. Egg laid in their nest.")),
            report(1), report(2), report(3)
        )

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        assertThat(result.reasons.any { it.contains("Owl") }).isTrue()
    }
}
