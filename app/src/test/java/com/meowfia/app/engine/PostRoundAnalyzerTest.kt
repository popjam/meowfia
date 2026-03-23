package com.meowfia.app.engine

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class PostRoundAnalyzerTest {

    @Before
    fun setup() {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
    }

    private fun createCoordinator(seed: Long = 42): GameCoordinator {
        return GameCoordinator(RandomProvider(seed))
    }

    private val testPool = listOf(
        PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT), PoolCard(RoleId.HAWK)
    )

    private val testNames = listOf("Alice", "Bob", "Charlie", "Diana")

    private fun runFullRound(
        coord: GameCoordinator,
        pool: List<PoolCard> = testPool,
        names: List<String> = testNames,
        forcedAlignments: Map<Int, Alignment>? = null,
        forcedRoles: Map<Int, RoleId>? = null,
        eliminatedId: Int = 0
    ) {
        coord.startNewRound(1, pool, names, 0)
        coord.assignRoles(forcedAlignments = forcedAlignments, forcedRoles = forcedRoles)

        for (i in names.indices) {
            val player = coord.state.players[i]
            val action = when (player.roleId) {
                RoleId.BLACK_SWAN -> NightAction.VisitSelf
                RoleId.TURKEY -> NightAction.NoVisit
                RoleId.MOSQUITO, RoleId.TIT -> NightAction.VisitRandom
                else -> NightAction.VisitPlayer((i + 1) % names.size)
            }
            coord.submitNightAction(i, action)
        }

        coord.resolveNight()

        // Generate all dawn reports
        for (i in names.indices) {
            coord.getDawnReport(i)
        }

        coord.startDay()
        coord.recordEggsecution(eliminatedId)
    }

    @Test
    fun analysis_has_correct_round_number() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.roundNumber).isEqualTo(1)
    }

    @Test
    fun analysis_contains_all_players() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.playerAssignments).hasSize(4)
        assertThat(analysis.playerAssignments.map { it.playerName })
            .containsExactly("Alice", "Bob", "Charlie", "Diana")
    }

    @Test
    fun analysis_pool_summary_includes_roles_and_flowers() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.poolSummary.roles).isNotEmpty()
        // No flowers in this pool
        assertThat(analysis.poolSummary.flowers).isEmpty()
    }

    @Test
    fun analysis_with_flower_pool() {
        val coord = createCoordinator()
        val poolWithFlower = listOf(
            PoolCard(RoleId.PIGEON), PoolCard(RoleId.HAWK), PoolCard(RoleId.WOLFSBANE)
        )
        runFullRound(coord, pool = poolWithFlower)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.poolSummary.flowers).containsExactly("Wolfsbane")
        assertThat(analysis.activeFlowers).hasSize(1)
        assertThat(analysis.activeFlowers[0].name).isEqualTo("Wolfsbane")
    }

    @Test
    fun analysis_night_walkthrough_has_entries_for_all_players() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.nightWalkthrough).hasSize(4)
    }

    @Test
    fun analysis_resolution_order_has_entries_for_all_players() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.resolutionOrder).hasSize(4)
    }

    @Test
    fun analysis_visit_map_has_entries_for_all_players() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.visitMap).hasSize(4)
    }

    @Test
    fun analysis_has_elimination_summary() {
        val coord = createCoordinator()
        runFullRound(
            coord,
            forcedAlignments = mapOf(
                0 to Alignment.MEOWFIA,
                1 to Alignment.FARM,
                2 to Alignment.FARM,
                3 to Alignment.FARM
            ),
            eliminatedId = 0
        )

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.eliminationSummary).isNotNull()
        assertThat(analysis.eliminationSummary!!.playerName).isEqualTo("Alice")
        assertThat(analysis.eliminationSummary!!.alignment).isEqualTo(Alignment.MEOWFIA)
        assertThat(analysis.eliminationSummary!!.wasCorrectElimination).isTrue()
    }

    @Test
    fun analysis_wrong_elimination() {
        val coord = createCoordinator()
        runFullRound(
            coord,
            forcedAlignments = mapOf(
                0 to Alignment.FARM,
                1 to Alignment.MEOWFIA,
                2 to Alignment.FARM,
                3 to Alignment.FARM
            ),
            eliminatedId = 0
        )

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.eliminationSummary!!.wasCorrectElimination).isFalse()
        assertThat(analysis.winningTeam).isEqualTo(Alignment.MEOWFIA)
    }

    @Test
    fun analysis_winning_team_set() {
        val coord = createCoordinator()
        runFullRound(
            coord,
            forcedAlignments = mapOf(
                0 to Alignment.MEOWFIA,
                1 to Alignment.FARM,
                2 to Alignment.FARM,
                3 to Alignment.FARM
            ),
            eliminatedId = 0
        )

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.winningTeam).isEqualTo(Alignment.FARM)
    }

    @Test
    fun analysis_player_assignments_show_alignment_and_role() {
        val coord = createCoordinator()
        runFullRound(
            coord,
            forcedAlignments = mapOf(
                0 to Alignment.FARM,
                1 to Alignment.MEOWFIA,
                2 to Alignment.FARM,
                3 to Alignment.FARM
            ),
            forcedRoles = mapOf(
                0 to RoleId.HAWK,
                1 to RoleId.HOUSE_CAT,
                2 to RoleId.PIGEON,
                3 to RoleId.PIGEON
            )
        )

        val analysis = coord.getPostRoundAnalysis()
        val aliceAssignment = analysis.playerAssignments.find { it.playerName == "Alice" }!!
        assertThat(aliceAssignment.alignment).isEqualTo(Alignment.FARM)
        assertThat(aliceAssignment.roleName).isEqualTo("Hawk")

        val bobAssignment = analysis.playerAssignments.find { it.playerName == "Bob" }!!
        assertThat(bobAssignment.alignment).isEqualTo(Alignment.MEOWFIA)
        assertThat(bobAssignment.roleName).isEqualTo("House Cat")
    }

    @Test
    fun analysis_hawk_finds_meowfia_shows_in_outcome() {
        val coord = createCoordinator()
        // Alice=Hawk visits Bob=Meowfia
        runFullRound(
            coord,
            forcedAlignments = mapOf(
                0 to Alignment.FARM,
                1 to Alignment.MEOWFIA,
                2 to Alignment.FARM,
                3 to Alignment.FARM
            ),
            forcedRoles = mapOf(
                0 to RoleId.HAWK,
                1 to RoleId.HOUSE_CAT,
                2 to RoleId.PIGEON,
                3 to RoleId.PIGEON
            )
        )

        val analysis = coord.getPostRoundAnalysis()
        val hawkEntry = analysis.nightWalkthrough.find { it.roleName == "Hawk" }!!
        assertThat(hawkEntry.outcome).contains("Meowfia")
        assertThat(hawkEntry.outcome).contains("egg")
    }

    @Test
    fun analysis_frog_swap_shows_role_changes() {
        val coord = createCoordinator()
        val frogPool = listOf(
            PoolCard(RoleId.FROG), PoolCard(RoleId.PIGEON), PoolCard(RoleId.HAWK)
        )
        // Alice=Frog visits Bob=Pigeon -> they swap
        runFullRound(
            coord,
            pool = frogPool,
            forcedAlignments = mapOf(
                0 to Alignment.FARM,
                1 to Alignment.FARM,
                2 to Alignment.FARM,
                3 to Alignment.FARM
            ),
            forcedRoles = mapOf(
                0 to RoleId.FROG,
                1 to RoleId.PIGEON,
                2 to RoleId.HAWK,
                3 to RoleId.PIGEON
            )
        )

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.roleChanges).isNotEmpty()
        // Alice should have changed from Frog to Pigeon
        val aliceChange = analysis.roleChanges.find { it.playerName == "Alice" }
        assertThat(aliceChange).isNotNull()
        assertThat(aliceChange!!.fromRole).isEqualTo("Frog")
        assertThat(aliceChange.toRole).isEqualTo("Pigeon")
    }

    @Test
    fun analysis_narrative_log_is_populated() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.narrativeLog).isNotEmpty()
    }

    @Test
    fun analysis_night_walkthrough_outcomes_are_not_empty() {
        val coord = createCoordinator()
        runFullRound(coord)

        val analysis = coord.getPostRoundAnalysis()
        for (entry in analysis.nightWalkthrough) {
            assertThat(entry.outcome).isNotEmpty()
        }
    }

    @Test
    fun analysis_dandelion_changes_resolution_order_note() {
        val coord = createCoordinator()
        val dandelionPool = listOf(
            PoolCard(RoleId.PIGEON), PoolCard(RoleId.HAWK), PoolCard(RoleId.DANDELION)
        )
        runFullRound(coord, pool = dandelionPool)

        val analysis = coord.getPostRoundAnalysis()
        assertThat(analysis.activeFlowers.any { it.name == "Dandelion" }).isTrue()
    }
}
