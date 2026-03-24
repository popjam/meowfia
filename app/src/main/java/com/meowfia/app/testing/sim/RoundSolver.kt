package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.GamePhase
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.NightResolver
import com.meowfia.app.util.RandomProvider

/**
 * Determines if a round is solvable by enumerating all possible Meowfia
 * subsets and checking which are consistent with the claimed egg deltas.
 *
 * Uses the real [NightResolver] to simulate egg flow for each hypothesis,
 * so new roles are automatically supported without changes here.
 */
object RoundSolver {

    enum class Solvability {
        /** Meowfia can be identified with certainty from public info. */
        SOLVED,
        /** Suspect list is smaller than random chance would give. */
        NARROWED,
        /** No useful deduction possible — effectively a coin flip. */
        COIN_FLIP
    }

    data class SolvabilityResult(
        val solvability: Solvability,
        /** Player IDs that appear as Meowfia in at least one consistent world. */
        val suspects: Set<Int>,
        /** Player IDs that are never Meowfia in any consistent world. */
        val cleared: Set<Int>,
        /** Number of consistent Meowfia subsets found. */
        val consistentWorlds: Int,
        /** Total candidate subsets evaluated. */
        val totalCandidates: Int,
        /** Human-readable reasons for the deduction. */
        val reasons: List<String>,
        val playerCount: Int,
        val meowfiaCount: Int,
        /** Each consistent world as a set of Meowfia player IDs. */
        val consistentWorldDetails: List<Set<Int>> = emptyList()
    )

    /**
     * Analyze a round by trying every possible Meowfia subset and simulating
     * egg flow with the real engine to check consistency with claimed deltas.
     *
     * @param claims Each player's day claim (role, target, egg delta).
     * @param pool The revealed pool for this round.
     * @param dawnReports Dawn reports (for additional info cross-referencing).
     * @param assignments Ground truth (for meowfiaCount; not used in deduction).
     * @param visitGraph The claimed visit graph.
     */
    fun analyze(
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        dawnReports: List<DawnReport>,
        assignments: List<PlayerAssignment>,
        visitGraph: Map<Int, Int?>
    ): SolvabilityResult {
        val playerIds = claims.keys.sorted()
        val playerCount = playerIds.size
        val meowfiaCount = assignments.count { it.alignment == Alignment.MEOWFIA }
        val reasons = mutableListOf<String>()

        // Quick pre-check: role claim conflicts
        val claimConflicts = findClaimConflicts(claims, pool)
        reasons.addAll(claimConflicts)

        // Enumerate all possible Meowfia subsets of size 0..playerCount
        // For typical games (4-8 players), this is at most 2^8 = 256 subsets
        val consistentMeowfiaPlayers = mutableSetOf<Int>()
        val consistentWorldList = mutableListOf<Set<Int>>()
        var totalCandidates = 0

        // Only consider worlds where Farm strictly outnumbers Meowfia.
        // Worlds where Meowfia >= Farm are excluded because Meowfia would
        // simply coordinate to win — deduction is meaningless.
        val maxMeowfiaSize = (playerCount - 1) / 2  // strictly less than half
        for (subsetSize in 0..maxMeowfiaSize) {
            for (meowfiaSubset in combinations(playerIds, subsetSize)) {
                totalCandidates++
                if (isConsistent(meowfiaSubset.toSet(), claims, pool, visitGraph, playerIds)) {
                    consistentWorldList.add(meowfiaSubset.toSet())
                    consistentMeowfiaPlayers.addAll(meowfiaSubset)
                }
            }
        }
        val consistentWorlds = consistentWorldList.size

        // Also run investigation cross-reference checks
        val investigationReasons = checkInvestigations(claims, dawnReports)
        reasons.addAll(investigationReasons)

        // Determine suspects and cleared
        val suspects = consistentMeowfiaPlayers
        val cleared = playerIds.filter { it !in suspects }.toSet()

        // Classify based on how many worlds were eliminated
        val eliminatedPct = if (totalCandidates > 0)
            (1.0 - consistentWorlds.toDouble() / totalCandidates) * 100 else 100.0

        val solvability = when {
            consistentWorlds == 0 -> {
                reasons.add("No consistent world found — someone's claims are impossible")
                Solvability.SOLVED
            }
            consistentWorlds == 1 -> {
                reasons.add("Exactly one consistent Meowfia assignment exists")
                Solvability.SOLVED
            }
            eliminatedPct >= 90 -> {
                reasons.add("${cleared.size} player(s) cleared, ${suspects.size} remain as suspects")
                Solvability.SOLVED
            }
            eliminatedPct > 0 -> {
                reasons.add("${cleared.size} player(s) cleared, ${suspects.size} remain as suspects")
                Solvability.NARROWED
            }
            else -> Solvability.COIN_FLIP
        }

        return SolvabilityResult(
            solvability = solvability,
            suspects = suspects,
            cleared = cleared,
            consistentWorlds = consistentWorlds,
            totalCandidates = totalCandidates,
            reasons = reasons,
            playerCount = playerCount,
            meowfiaCount = meowfiaCount,
            consistentWorldDetails = consistentWorldList
        )
    }

    /**
     * Check if a hypothetical Meowfia subset is consistent with all claims.
     * Farm players are assumed truthful; Meowfia players' claims are ignored.
     * We simulate the egg flow using the Farm players' claimed roles and targets,
     * then check if the resulting egg deltas match their claimed deltas.
     */
    private fun isConsistent(
        meowfiaSet: Set<Int>,
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        visitGraph: Map<Int, Int?>,
        playerIds: List<Int>
    ): Boolean {
        // Step 1: Check role assignment feasibility
        // Farm players' claimed roles must be assignable from the pool
        val farmClaims = claims.filter { it.key !in meowfiaSet }
        val claimedFarmRoles = farmClaims.values.map { it.claimedRole }

        // Non-buffer roles can only be claimed once — unless Twinflower is in pool
        val hasTwinflower = RoleId.TWINFLOWER in pool
        if (!hasTwinflower) {
            val nonBufferCounts = claimedFarmRoles.filter { !it.isBuffer }.groupingBy { it }.eachCount()
            val poolCounts = pool.filter { !it.isFlower }.groupingBy { it }.eachCount()
            for ((role, count) in nonBufferCounts) {
                val available = (poolCounts[role] ?: 0) + if (role.isBuffer) Int.MAX_VALUE else 0
                if (count > maxOf(1, available)) return false
            }
        }

        // Step 2: Simulate egg flow using the real engine
        // Build a hypothetical game state where Farm players have their claimed roles
        // and Meowfia players have House Cat (the buffer Meowfia role)
        val players = playerIds.map { id ->
            val isMeowfia = id in meowfiaSet
            val claim = claims[id]!!
            Player(
                id = id,
                name = "P$id",
                alignment = if (isMeowfia) Alignment.MEOWFIA else Alignment.FARM,
                roleId = if (isMeowfia) RoleId.HOUSE_CAT else claim.claimedRole
            )
        }

        // Build visit graph from claims (Farm players' claims are truth, Meowfia pick random)
        val simVisitGraph = mutableMapOf<Int, Int?>()
        for (id in playerIds) {
            val claim = claims[id]!!
            simVisitGraph[id] = claim.claimedTargetId
        }

        val gameState = GameState(
            roundNumber = 1,
            players = players,
            pool = pool.map { PoolCard(it) },
            dealerSeat = 0,
            nightActions = playerIds.associateWith { id ->
                val claim = claims[id]!!
                when {
                    claim.claimedTargetId != null -> NightAction.VisitPlayer(claim.claimedTargetId)
                    claim.claimedRole == RoleId.BLACK_SWAN -> NightAction.VisitSelf
                    claim.claimedRole == RoleId.TURKEY -> NightAction.NoVisit
                    claim.claimedRole in setOf(RoleId.MOSQUITO, RoleId.TIT) -> NightAction.VisitRandom
                    else -> NightAction.NoVisit
                }
            },
            nightResults = emptyMap(),
            dawnReports = emptyMap(),
            activeFlowers = emptyList(),
            visitGraph = simVisitGraph,
            phase = GamePhase.NIGHT,
            cawCawCount = 0,
            eliminatedPlayerId = null
        )

        // Run the real night resolver
        val resolver = NightResolver(RandomProvider(0))
        val context = resolver.resolve(gameState)

        // Step 3: Check if computed egg deltas match Farm players' claimed deltas
        for ((id, claim) in farmClaims) {
            val computedDelta = context.getClampedEggDelta(id)
            if (computedDelta != claim.claimedEggDelta) return false
        }

        return true
    }

    /** Find obvious claim conflicts (duplicate non-buffer roles, roles not in pool). */
    private fun findClaimConflicts(
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>
    ): List<String> {
        val reasons = mutableListOf<String>()
        val poolRoles = pool.filter { !it.isFlower }.toSet() + setOf(RoleId.PIGEON, RoleId.HOUSE_CAT)

        for ((playerId, claim) in claims) {
            if (claim.claimedRole !in poolRoles && !claim.claimedRole.isBuffer) {
                reasons.add("Player $playerId claims ${claim.claimedRole.displayName} not in pool")
            }
        }

        // Duplicate non-buffer roles are suspicious — unless Twinflower allows it
        val hasTwinflower = RoleId.TWINFLOWER in pool
        if (!hasTwinflower) {
            val roleClaims = claims.entries.groupBy { it.value.claimedRole }
                .filter { (role, claimants) -> !role.isBuffer && claimants.size > 1 }
            for ((role, claimants) in roleClaims) {
                reasons.add("${claimants.size} players claim ${role.displayName}")
            }
        }

        return reasons
    }

    /** Cross-reference Owl investigation info with visit claims. */
    private fun checkInvestigations(
        claims: Map<Int, ClaimData>,
        dawnReports: List<DawnReport>
    ): List<String> {
        val reasons = mutableListOf<String>()

        for ((playerId, claim) in claims) {
            if (claim.claimedRole != RoleId.OWL) continue
            val report = dawnReports.find { it.playerId == playerId } ?: continue
            val targetId = claim.claimedTargetId ?: continue

            for (info in report.additionalInfo) {
                if (info.contains("No animals visited")) {
                    val visitorsToTarget = claims.entries
                        .filter { it.value.claimedTargetId == targetId && it.key != playerId }
                    if (visitorsToTarget.isNotEmpty()) {
                        reasons.add("Owl says nobody visited $targetId but ${visitorsToTarget.size} claim they did")
                    }
                }
            }
        }

        return reasons
    }

    /** Generate all combinations of [size] elements from [items]. */
    private fun <T> combinations(items: List<T>, size: Int): List<List<T>> {
        if (size == 0) return listOf(emptyList())
        if (size > items.size) return emptyList()
        if (size == items.size) return listOf(items)

        val result = mutableListOf<List<T>>()
        fun recurse(start: Int, current: List<T>) {
            if (current.size == size) {
                result.add(current)
                return
            }
            for (i in start until items.size) {
                recurse(i + 1, current + items[i])
            }
        }
        recurse(0, emptyList())
        return result
    }
}

/**
 * Structured claim data extracted from a player's day claim.
 */
data class ClaimData(
    val playerId: Int,
    val claimedRole: RoleId,
    val claimedTargetId: Int?,
    val claimedEggDelta: Int
)
