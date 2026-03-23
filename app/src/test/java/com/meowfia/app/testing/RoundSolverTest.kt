package com.meowfia.app.testing

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.ClaimData
import com.meowfia.app.testing.sim.RoundSolver
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoundSolverTest {

    private fun report(id: Int, delta: Int = 0, info: List<String> = emptyList()) =
        DawnReport(playerId = id, reportedEggDelta = delta, actualEggDelta = delta, additionalInfo = info)

    // --- Role conflict detection ---

    @Test
    fun duplicate_non_buffer_role_flags_suspects() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.HAWK, 1, 1),
            1 to ClaimData(1, RoleId.HAWK, 2, 0),  // Also claims Hawk!
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 1)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.HAWK),
            PlayerAssignment(1, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        // Both Hawk claimants should be flagged
        assertThat(result.suspects).containsAtLeast(0, 1)
        assertThat(result.reasons.any { it.contains("Hawk") }).isTrue()
    }

    @Test
    fun role_not_in_pool_flags_suspect() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.OWL, 1, 0),  // Owl not in pool!
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.HAWK, 1, 1)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.HAWK)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        assertThat(result.suspects).contains(0)
    }

    // --- Egg accounting ---

    @Test
    fun impossible_egg_total_flags_suspects() {
        // Pool can produce max ~4 eggs but players claim way more
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 3),  // Claims +3 — suspicious
            1 to ClaimData(1, RoleId.HAWK, 2, 2),
            2 to ClaimData(2, RoleId.PIGEON, 0, 1),
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
        assertThat(result.suspects).contains(0) // Highest delta player flagged
    }

    // --- Target consistency ---

    @Test
    fun turkey_claiming_to_visit_someone_is_flagged() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.TURKEY)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.TURKEY, 1, 0),  // Turkey shouldn't visit!
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        assertThat(result.suspects).contains(0)
        assertThat(result.reasons.any { it.contains("Turkey") }).isTrue()
    }

    // --- Owl investigation cross-reference ---

    @Test
    fun owl_sees_no_visitors_but_someone_claims_to_visit_flags_conflict() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.OWL)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.OWL, 2, 0),    // Owl visits player 2
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),  // Pigeon also visits player 2
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.OWL),
            PlayerAssignment(1, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        // Owl's dawn report says no animals visited player 2
        val reports = listOf(
            report(0, info = listOf("No animals visited player 2. Egg laid in their nest.")),
            report(1), report(2), report(3)
        )

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        // Player 1 claims to visit player 2, but Owl says nobody did
        assertThat(result.suspects).contains(1)
    }

    // --- Classification ---

    @Test
    fun clean_round_with_no_conflicts_is_coin_flip() {
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 0),
            1 to ClaimData(1, RoleId.HAWK, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 0, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.HAWK),
            PlayerAssignment(2, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        // Meowfia player 2 claimed Pigeon with 0 eggs — perfectly plausible
        // No conflicts → coin flip
        assertThat(result.solvability).isAnyOf(
            RoundSolver.Solvability.COIN_FLIP,
            RoundSolver.Solvability.NARROWED
        )
    }
}
