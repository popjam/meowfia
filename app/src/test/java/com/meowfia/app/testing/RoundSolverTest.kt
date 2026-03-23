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
    fun impossible_egg_delta_eliminates_worlds() {
        // Player 0 claims Pigeon visiting player 1 with +3 eggs — impossible for Pigeon
        // The real engine would compute 0 self-eggs for Pigeon, so if they claim +3
        // there must be 3 eggs from visitors. If the pool can't produce that, inconsistent.
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 3),  // claims +3, very suspicious
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
        // The solver should find that worlds where player 0 is Farm are inconsistent
        // because the engine can't produce +3 for a Pigeon with this pool
        assertThat(result.solvability).isNotEqualTo(RoundSolver.Solvability.COIN_FLIP)
    }

    @Test
    fun consistent_claims_produce_multiple_worlds() {
        // All claims are plausible — many worlds should be consistent
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 0),
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        // Multiple Meowfia subsets should be consistent (all claim Pigeon with 0 delta)
        assertThat(result.consistentWorlds).isGreaterThan(1)
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
        // 3 players → 2^3 = 8 total subsets (all sizes 0,1,2,3)
        assertThat(result.totalCandidates).isEqualTo(8)
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
