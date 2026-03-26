package com.meowfia.app.testing

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.GamePhase
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.engine.NightResolver
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.testing.sim.ClaimData
import com.meowfia.app.testing.sim.RoundSolver
import com.meowfia.app.util.RandomProvider
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
    fun impossible_eagle_egg_delta_eliminates_worlds() {
        // Player 0 claims Eagle visiting player 1 with +5 eggs.
        // Only 3 other players exist, and only 1 claims to visit player 1.
        // Even with 1 Meowfia, max visitors to player 1 = 1 known + 1 Meowfia = 2.
        // +5 is impossible → worlds where player 0 is Farm are eliminated.
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.EAGLE)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.EAGLE, 1, 5),  // claims +5 as Eagle — too many
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 3, 0),
            3 to ClaimData(3, RoleId.PIGEON, 1, 0)  // only player visiting target 1
        )
        val assignments = listOf(
            PlayerAssignment(0, Alignment.MEOWFIA, RoleId.HOUSE_CAT),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(3, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..3).map { report(it) }

        val result = RoundSolver.analyze(claims, pool, reports, assignments, emptyMap())
        assertThat(result.totalCandidates).isEqualTo(5)
        // Player 0's +5 Eagle claim is impossible — should eliminate worlds where they're Farm
        assertThat(result.consistentWorlds).isLessThan(result.totalCandidates)
        // Player 0 should be a suspect
        assertThat(result.suspects).contains(0)
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
    fun three_pigeons_circle_is_solved() {
        // 3 Pigeons in a circle: P0→P1, P1→P2, P2→P0
        // Each player gets +1 egg from the pigeon visiting them.
        // If any player is Meowfia (House Cat), they don't lay eggs,
        // so their target loses an egg → inconsistent with +1 claim.
        // Only the 0-Meowfia world should be consistent → SOLVED.
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 1),
            1 to ClaimData(1, RoleId.PIGEON, 2, 1),
            2 to ClaimData(2, RoleId.PIGEON, 0, 1)
        )
        val visitGraph = mapOf<Int, Int?>(0 to 1, 1 to 2, 2 to 0)
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = (0..2).map { report(it, delta = 1) }

        val result = RoundSolver.analyze(
            claims, pool, reports, assignments, visitGraph,
            actualVisitGraph = visitGraph,
            exhaustiveSearch = true
        )
        println("=== 3 Pigeons Circle ===")
        println("Consistent worlds: ${result.consistentWorlds}")
        println("Total candidates: ${result.totalCandidates}")
        println("Solvability: ${result.solvability}")
        println("Suspects: ${result.suspects}")
        println("Cleared: ${result.cleared}")
        println("Reasons: ${result.reasons}")
        println("World details: ${result.consistentWorldDetails}")

        assertThat(result.totalCandidates).isEqualTo(4) // C(3,0) + C(3,1) = 1 + 3
        assertThat(result.consistentWorlds).isEqualTo(1) // only 0-Meowfia world
        assertThat(result.solvability).isEqualTo(RoundSolver.Solvability.SOLVED)
        assertThat(result.cleared).containsExactly(0, 1, 2)
        assertThat(result.suspects).isEmpty()
    }

    @Test
    fun three_pigeons_two_visit_third() {
        // P0→P2, P1→P2, P2→P0
        // P0 delta: +1 (from P2), P1 delta: 0 (nobody visits), P2 delta: +2 (from P0 + P1)
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 2, 1),
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 0, 2)
        )
        val visitGraph = mapOf<Int, Int?>(0 to 2, 1 to 2, 2 to 0)
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.FARM, RoleId.PIGEON)
        )
        val reports = listOf(report(0, delta = 1), report(1, delta = 0), report(2, delta = 2))

        val result = RoundSolver.analyze(
            claims, pool, reports, assignments, visitGraph,
            actualVisitGraph = visitGraph,
            exhaustiveSearch = true
        )
        println("=== 3 Pigeons Two-Visit-Third ===")
        println("Consistent worlds: ${result.consistentWorlds}")
        println("Total candidates: ${result.totalCandidates}")
        println("Solvability: ${result.solvability}")
        println("Suspects: ${result.suspects}")
        println("Cleared: ${result.cleared}")
        println("Reasons: ${result.reasons}")
        println("World details: ${result.consistentWorldDetails}")
    }

    @Test
    fun two_pigeons_one_housecat_solved_case() {
        // Circle: P0→P1, P1→P2, P2→P0. House Cat is P2.
        // P0: +1 (from P1), P1: +1 (from P0, HC doesn't lay), P2: +1 (from P1)
        // Wait - P2 is HC so claims are lies. Let's set claims as what everyone SAYS.
        // P2 (HC) claims Pigeon→P0, delta +1
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 1),
            1 to ClaimData(1, RoleId.PIGEON, 2, 0),
            2 to ClaimData(2, RoleId.PIGEON, 0, 1)
        )
        val visitGraph = mapOf<Int, Int?>(0 to 1, 1 to 2, 2 to 0)
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.MEOWFIA, RoleId.HOUSE_CAT)
        )
        val reports = listOf(report(0, delta = 1), report(1, delta = 0), report(2, delta = 1))

        val result = RoundSolver.analyze(
            claims, pool, reports, assignments, visitGraph,
            actualVisitGraph = visitGraph, exhaustiveSearch = true
        )
        println("=== 2 Pigeons + 1 HC (circle) ===")
        println("Solvability: ${result.solvability}")
        println("Consistent worlds: ${result.consistentWorlds} / ${result.totalCandidates}")
        println("Suspects: ${result.suspects}")
        println("Cleared: ${result.cleared}")
        println("World details: ${result.consistentWorldDetails}")
    }

    @Test
    fun two_pigeons_one_housecat_coinflip_case() {
        // P0→P1, P1→P0, P2→P1. House Cat is P2.
        // Real deltas: P0 +1 (from P1), P1 +1 (from P0, HC doesn't lay), P2 0 (nobody visits)
        // P2 (HC) claims Pigeon→P1, delta 0
        // If P0 were HC instead: Farm P1 gets +1 (from P2), Farm P2 gets 0. Same deltas!
        val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
        val claims = mapOf(
            0 to ClaimData(0, RoleId.PIGEON, 1, 1),
            1 to ClaimData(1, RoleId.PIGEON, 0, 1),
            2 to ClaimData(2, RoleId.PIGEON, 1, 0)
        )
        val visitGraph = mapOf<Int, Int?>(0 to 1, 1 to 0, 2 to 1)
        val assignments = listOf(
            PlayerAssignment(0, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(1, Alignment.FARM, RoleId.PIGEON),
            PlayerAssignment(2, Alignment.MEOWFIA, RoleId.HOUSE_CAT)
        )
        val reports = listOf(report(0, delta = 1), report(1, delta = 1), report(2, delta = 0))

        val result = RoundSolver.analyze(
            claims, pool, reports, assignments, visitGraph,
            actualVisitGraph = visitGraph, exhaustiveSearch = true
        )
        println("=== 2 Pigeons + 1 HC (overlap targets) ===")
        println("Solvability: ${result.solvability}")
        println("Consistent worlds: ${result.consistentWorlds} / ${result.totalCandidates}")
        println("Suspects: ${result.suspects}")
        println("Cleared: ${result.cleared}")
        println("World details: ${result.consistentWorldDetails}")
    }

    // ==================== Solvability Harness ====================
    //
    // Reusable engine that exhaustively tests every visit pattern for a given
    // set of Farm roles + 1 House Cat, then prints a scorecard.
    //
    // Usage: call `solvabilityScorecard(farmRoles)` with 2+ Farm roles.
    // The last player is always the House Cat.
    //
    // To compare multiple compositions, call `compareSolvability(vararg compositions)`.

    data class ScorecardResult(
        val label: String,
        val farmRoles: List<RoleId>,
        val solved: Int,
        val actionable: Int,
        val narrowed: Int,
        val coinFlip: Int,
        val total: Int,
        val details: List<String>
    ) {
        val solvedPct get() = if (total > 0) solved * 100.0 / total else 0.0
    }

    /**
     * Run every visit-pattern permutation for the given Farm roles + 1 Meowfia.
     * Uses the real NightResolver to compute ground-truth egg deltas, then feeds
     * claims into the RoundSolver to check solvability.
     *
     * @param farmRoles The Farm roles (e.g. [PIGEON, PIGEON] or [PIGEON, HAWK, EAGLE]).
     *                  The Meowfia player is added automatically as the last player.
     * @param meowfiaRole The actual Meowfia role dealt (default HOUSE_CAT).
     * @param meowfiaClaimsRole What the Meowfia player claims to be (default PIGEON).
     * @param extraPoolRoles Additional roles visible in the pool but not dealt to anyone.
     *                       Use this to add Meowfia roles (e.g. FLOOFER) that the solver
     *                       should consider as possible assignments, or extra Farm roles
     *                       that affect duplicate-role feasibility checks.
     */
    /**
     * @param meowfiaCount Number of Meowfia players (last N players are Meowfia).
     * @param meowfiaRoles Specific Meowfia roles to assign (length must equal meowfiaCount).
     *                     Defaults to all House Cat.
     * @param sampleSize If non-null, randomly sample this many visit patterns instead of
     *                   exhaustively testing all. Use for large player counts (7+).
     */
    private fun solvabilityScorecard(
        farmRoles: List<RoleId>,
        meowfiaRole: RoleId = RoleId.HOUSE_CAT,
        meowfiaClaimsRole: RoleId = RoleId.PIGEON,
        extraPoolRoles: List<RoleId> = emptyList(),
        meowfiaCount: Int = 1,
        meowfiaRoles: List<RoleId>? = null,
        sampleSize: Int? = null
    ): ScorecardResult {
        val actualMeowfiaRoles = meowfiaRoles ?: List(meowfiaCount) { meowfiaRole }
        require(actualMeowfiaRoles.size == meowfiaCount)

        val playerCount = farmRoles.size + meowfiaCount
        val meowfiaIndices = (farmRoles.size until playerCount).toSet()
        val playerIds = (0 until playerCount).toList()

        // Build pool: roles in play + buffers + any extra visible roles
        val pool = ((farmRoles + actualMeowfiaRoles).distinct() +
            listOf(RoleId.PIGEON, RoleId.HOUSE_CAT).filter { it !in farmRoles && it !in actualMeowfiaRoles } +
            extraPoolRoles).distinct()

        // Valid targets per player: everyone except self
        val allTargets = playerIds.map { id -> playerIds.filter { it != id } }

        // Generate visit patterns: exhaustive or sampled
        val visitPatterns = if (sampleSize != null) {
            val rng = java.util.Random(42) // deterministic seed for reproducibility
            List(sampleSize) {
                playerIds.map { id -> allTargets[id].let { targets -> targets[rng.nextInt(targets.size)] } }
            }
        } else {
            cartesianProduct(allTargets)
        }

        var solved = 0
        var actionable = 0
        var narrowed = 0
        var coinFlip = 0
        val details = mutableListOf<String>()

        for (pattern in visitPatterns) {
            // Build the real game state
            val players = playerIds.map { id ->
                if (id in meowfiaIndices) {
                    val mIdx = id - farmRoles.size
                    Player(id, "P$id", Alignment.MEOWFIA, actualMeowfiaRoles[mIdx])
                } else {
                    Player(id, "P$id", Alignment.FARM, farmRoles[id])
                }
            }
            val visitGraph = playerIds.associateWith<Int, Int?> { pattern[it] }
            val nightActions = playerIds.associateWith<Int, NightAction> { id ->
                NightAction.VisitPlayer(pattern[id])
            }

            val gs = GameState(
                roundNumber = 1, players = players,
                pool = pool.map { PoolCard(it) }, dealerSeat = 0,
                nightActions = nightActions, nightResults = emptyMap(),
                dawnReports = emptyMap(), activeFlowers = emptyList(),
                visitGraph = visitGraph, phase = GamePhase.NIGHT,
                cawCawCount = 0, eliminatedPlayerId = null
            )
            val ctx = NightResolver(RandomProvider(0)).resolve(gs)
            val deltas = playerIds.map { ctx.getClampedEggDelta(it) }

            // Build claims: Farm players tell truth, Meowfia claims buffer role
            val claims = playerIds.associate { id ->
                val role = if (id in meowfiaIndices) meowfiaClaimsRole else farmRoles[id]
                id to ClaimData(id, role, pattern[id], deltas[id])
            }
            val assignments = playerIds.map { id ->
                if (id in meowfiaIndices) {
                    val mIdx = id - farmRoles.size
                    PlayerAssignment(id, Alignment.MEOWFIA, actualMeowfiaRoles[mIdx])
                } else {
                    PlayerAssignment(id, Alignment.FARM, farmRoles[id])
                }
            }
            val reports = playerIds.map { report(it, delta = deltas[it]) }

            val result = RoundSolver.analyze(
                claims, pool, reports, assignments, visitGraph,
                actualVisitGraph = visitGraph, exhaustiveSearch = true
            )

            val patternStr = playerIds.joinToString(" ") { "P$it→P${pattern[it]}" }
            val label = result.verdictLabel
            details.add("$patternStr  deltas=$deltas  → $label  worlds=${result.consistentWorlds}")

            when (result.solvability) {
                RoundSolver.Solvability.SOLVED -> solved++
                RoundSolver.Solvability.ACTIONABLE -> actionable++
                RoundSolver.Solvability.NARROWED -> narrowed++
                RoundSolver.Solvability.COIN_FLIP -> coinFlip++
            }
        }

        // Build compact label: group duplicates (e.g., "6P + Hawk + HC")
        val allRoles = farmRoles + actualMeowfiaRoles
        val roleCounts = allRoles.groupingBy { it }.eachCount()
        val label = roleCounts.entries.joinToString(" + ") { (role, count) ->
            val short = when (role) {
                RoleId.PIGEON -> "P"
                RoleId.HOUSE_CAT -> "HC"
                RoleId.HAWK -> "Hawk"
                RoleId.OWL -> "Owl"
                RoleId.EAGLE -> "Eagle"
                RoleId.TURKEY -> "Turkey"
                RoleId.FALCON -> "Falcon"
                RoleId.CHICKEN -> "Chicken"
                RoleId.SHEEPDOG -> "Sheep"
                RoleId.FLOOFER -> "Floof"
                RoleId.TOP_CAT -> "TopCat"
                RoleId.MOSQUITO -> "Mosq"
                else -> role.displayName.take(6)
            }
            if (count > 1) "${count}$short" else short
        }
        return ScorecardResult(label, farmRoles, solved, actionable, narrowed, coinFlip, visitPatterns.size, details)
    }

    /**
     * Compare multiple role compositions side-by-side.
     * Prints a summary table sorted by solved percentage.
     */
    private fun compareSolvability(
        vararg compositions: List<RoleId>,
        meowfiaRole: RoleId = RoleId.HOUSE_CAT,
        meowfiaClaimsRole: RoleId = RoleId.PIGEON,
        extraPoolRoles: List<RoleId> = emptyList(),
        meowfiaCount: Int = 1,
        meowfiaRoles: List<RoleId>? = null,
        sampleSize: Int? = null,
        printDetails: Boolean = false
    ): List<ScorecardResult> {
        val results = compositions.map { farmRoles ->
            solvabilityScorecard(farmRoles, meowfiaRole, meowfiaClaimsRole, extraPoolRoles, meowfiaCount, meowfiaRoles, sampleSize)
        }

        val maxLabel = maxOf(11, results.maxOf { it.label.length })
        val w = maxLabel + 38  // total inner width
        println()
        println("╔${"═".repeat(w)}╗")
        println("║${"SOLVABILITY COMPARISON".padStart((w + 21) / 2).padEnd(w)}║")
        println("╠${"═".repeat(w)}╣")
        println("║ %-${maxLabel}s %7s %7s %7s %7s ║".format("Composition", "SOLVED", "ACTION", "NARROW", "FLIP"))
        println("╠${"═".repeat(w)}╣")

        for (r in results.sortedByDescending { it.solvedPct }) {
            println("║ %-${maxLabel}s %3d/%-3d %3d/%-3d %3d/%-3d %3d/%-3d ║".format(
                r.label.take(maxLabel),
                r.solved, r.total,
                r.actionable, r.total,
                r.narrowed, r.total,
                r.coinFlip, r.total
            ))
        }
        println("╚${"═".repeat(w)}╝")
        println()

        if (printDetails) {
            for (r in results) {
                println("--- ${r.label} ---")
                r.details.forEach { println("  $it") }
                println()
            }
        }

        return results
    }

    /** Cartesian product of lists of valid targets. */
    private fun cartesianProduct(lists: List<List<Int>>): List<List<Int>> {
        if (lists.isEmpty()) return listOf(emptyList())
        val result = mutableListOf<List<Int>>()
        fun recurse(index: Int, current: List<Int>) {
            if (index == lists.size) {
                result.add(current)
                return
            }
            for (item in lists[index]) {
                recurse(index + 1, current + item)
            }
        }
        recurse(0, emptyList())
        return result
    }

    // ==================== Comparison Tests ====================

    @Test
    fun compare_3player_compositions() {
        // All 2-Farm + 1-HC combos with implemented Farm roles
        val farmRoles = listOf(
            RoleId.PIGEON, RoleId.HAWK, RoleId.OWL, RoleId.EAGLE,
            RoleId.TURKEY, RoleId.FALCON, RoleId.CHICKEN
        )

        // Every pair (including duplicates like Pigeon+Pigeon)
        val compositions = mutableListOf<List<RoleId>>()
        for (i in farmRoles.indices) {
            for (j in i until farmRoles.size) {
                compositions.add(listOf(farmRoles[i], farmRoles[j]))
            }
        }

        compareSolvability(*compositions.toTypedArray(), printDetails = false)
    }

    @Test
    fun compare_4player_compositions() {
        // 3-Farm + 1-HC — try a few interesting trios
        val compositions = arrayOf(
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.HAWK, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.HAWK, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.HAWK, RoleId.FALCON),
            listOf(RoleId.PIGEON, RoleId.EAGLE, RoleId.TURKEY),
            listOf(RoleId.HAWK, RoleId.OWL, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.OWL, RoleId.FALCON)
        )

        compareSolvability(*compositions, printDetails = false)
    }

    @Test
    fun compare_pigeon_plus_every_farm_role() {
        // Pigeon + <every implemented Farm role> + House Cat at 3 players
        val implementedFarm = RoleId.entries.filter {
            it.isFarmAnimal && it.implemented && it != RoleId.PIGEON
        }
        val compositions = implementedFarm.map { role ->
            listOf(RoleId.PIGEON, role)
        }
        compareSolvability(*compositions.toTypedArray(), printDetails = false)
    }

    @Test
    fun compare_two_pigeons_plus_every_farm_role() {
        // Pigeon + Pigeon + <every implemented Farm role> + House Cat at 4 players
        val implementedFarm = RoleId.entries.filter {
            it.isFarmAnimal && it.implemented
        }
        val compositions = implementedFarm.map { role ->
            listOf(RoleId.PIGEON, RoleId.PIGEON, role)
        }
        compareSolvability(*compositions.toTypedArray(), printDetails = false)
    }

    @Test
    fun compare_floofer_impact() {
        // How does having Floofer in the pool change solvability?
        // When the solver sees Floofer in the pool, it tries assigning it
        // to hypothetical Meowfia players — hugs block Farm actions, changing deltas.
        // Compare: same Farm roles, but pool has HC only vs HC + Floofer.
        println("=== Without Floofer in pool (HC only) ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.PIGEON),
            listOf(RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.CHICKEN),
            printDetails = false
        )
        println("=== With Floofer in pool (HC + Floofer) ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.PIGEON),
            listOf(RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.CHICKEN),
            extraPoolRoles = listOf(RoleId.FLOOFER),
            printDetails = false
        )
        println("=== Real Meowfia IS Floofer ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.PIGEON),
            listOf(RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.CHICKEN),
            meowfiaRole = RoleId.FLOOFER,
            printDetails = false
        )
    }

    @Test
    fun compare_sheepdog_vs_floofer() {
        val farmComps = arrayOf(
            listOf(RoleId.PIGEON, RoleId.PIGEON),
            listOf(RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.CHICKEN),
            listOf(RoleId.PIGEON, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.TURKEY)
        )

        println("=== Baseline: HC only in pool ===")
        compareSolvability(*farmComps, printDetails = false)

        println("=== HC + Sheepdog in pool (real Meowfia = HC) ===")
        compareSolvability(*farmComps, extraPoolRoles = listOf(RoleId.SHEEPDOG), printDetails = false)

        println("=== HC + Floofer in pool (real Meowfia = HC) ===")
        compareSolvability(*farmComps, extraPoolRoles = listOf(RoleId.FLOOFER), printDetails = false)

        println("=== Real Meowfia = Floofer ===")
        compareSolvability(*farmComps, meowfiaRole = RoleId.FLOOFER, printDetails = false)

        // Now test with Sheepdog as a Farm role alongside hugger Meowfia
        println("=== Farm has Sheepdog, Meowfia = HC ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.SHEEPDOG),
            listOf(RoleId.HAWK, RoleId.SHEEPDOG),
            listOf(RoleId.CHICKEN, RoleId.SHEEPDOG),
            printDetails = false
        )

        println("=== Farm has Sheepdog, Meowfia = Floofer ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.SHEEPDOG),
            listOf(RoleId.HAWK, RoleId.SHEEPDOG),
            listOf(RoleId.CHICKEN, RoleId.SHEEPDOG),
            meowfiaRole = RoleId.FLOOFER,
            printDetails = false
        )
    }

    @Test
    fun custom_solvability_comparison() {
        // Top Cat vs House Cat: does alignment flipping change solvability?
        // Test with alignment-sensitive roles (Hawk, Lovebird) vs not (Pigeon-only)
        println("=== Pigeon-only: Top Cat flip is invisible ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.PIGEON),
            meowfiaRole = RoleId.TOP_CAT,
            printDetails = true
        )
        println("=== Pigeon-only: House Cat baseline ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.PIGEON),
            meowfiaRole = RoleId.HOUSE_CAT,
            printDetails = true
        )
        println("=== With Hawk: Top Cat flip may be detectable ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.HAWK),
            meowfiaRole = RoleId.TOP_CAT,
            printDetails = true
        )
        println("=== With Hawk: House Cat baseline ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.HAWK),
            meowfiaRole = RoleId.HOUSE_CAT,
            printDetails = true
        )
        println("=== With Lovebird: Top Cat flip should matter ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.LOVEBIRD),
            meowfiaRole = RoleId.TOP_CAT,
            printDetails = true
        )
        println("=== With Lovebird: House Cat baseline ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.LOVEBIRD),
            meowfiaRole = RoleId.HOUSE_CAT,
            printDetails = true
        )
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

    // ==================== ACTIONABLE Tests (2 Meowfia) ====================

    @Test
    fun actionable_2meowfia_pigeons_plus_farm_roles() {
        // 2 House Cats + Farm roles. With 2 Meowfia, ACTIONABLE should appear
        // when the solver can pin down at least 1 of the 2 even if not both.
        val implementedFarm = listOf(
            RoleId.PIGEON, RoleId.HAWK, RoleId.OWL, RoleId.EAGLE,
            RoleId.TURKEY, RoleId.CHICKEN, RoleId.FALCON
        )

        // 3 Farm + 2 HC = 5 players. Warning: 4^5 = 1024 patterns × many worlds.
        // Keep Farm compositions small to stay fast.
        println("=== 3 Farm + 2 House Cat (5 players) ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.CHICKEN),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.TURKEY),
            listOf(RoleId.PIGEON, RoleId.HAWK, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.HAWK, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.CHICKEN, RoleId.HAWK),
            meowfiaCount = 2,
            printDetails = false
        )
    }

    @Test
    fun actionable_0meowfia_just_farm() {
        // 3 Farm players, 0 Meowfia. Every pattern should be SOLVED
        // (all worlds agree: 0 Meowfia = vote nobody).
        println("=== 3 Farm + 0 Meowfia ===")
        compareSolvability(
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.CHICKEN),
            listOf(RoleId.PIGEON, RoleId.HAWK, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.HAWK, RoleId.EAGLE),
            meowfiaCount = 0,
            printDetails = false
        )
    }

    @Test
    fun compare_8player_compositions() {
        // 7 Farm + 1 House Cat = 8 players. 7^8 ≈ 5.7M patterns — sample 500.
        val samples = 500
        println("=== 8 players (7 Farm + 1 HC), $samples sampled patterns ===")
        compareSolvability(
            // All Pigeons
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON),
            // 6 Pigeons + 1 specialty
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.CHICKEN),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.TURKEY),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.FALCON),
            // Mixed compositions
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK, RoleId.OWL),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK, RoleId.OWL, RoleId.EAGLE),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.CHICKEN, RoleId.HAWK),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK, RoleId.CHICKEN, RoleId.TURKEY),
            listOf(RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.PIGEON, RoleId.HAWK, RoleId.CHICKEN),
            sampleSize = samples,
            printDetails = false
        )
    }
}
