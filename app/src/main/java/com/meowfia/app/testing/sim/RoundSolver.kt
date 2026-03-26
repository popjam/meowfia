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
import com.meowfia.app.data.registry.RoleRegistry
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

        // SOLVED requires all consistent worlds to agree on the exact same Meowfia set.
        val allWorldsAgree = consistentWorldList.isNotEmpty() &&
            consistentWorldList.distinct().size == 1

        val solvability = when {
            consistentWorlds == 0 -> {
                reasons.add("No consistent world found — someone's claims are impossible")
                Solvability.SOLVED
            }
            allWorldsAgree -> {
                reasons.add("All consistent worlds agree on the same Meowfia set")
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
     *
     * The solver's contract: a subset is consistent if there exists **any**
     * plausible combination of (Meowfia role from pool) × (valid visit target)
     * that explains what Farm players reported. This requires enumerating over
     * possible Meowfia role assignments and visit targets.
     */
    private fun isConsistent(
        meowfiaSet: Set<Int>,
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        visitGraph: Map<Int, Int?>,
        playerIds: List<Int>
    ): Boolean {
        // Step 1: Check role assignment feasibility
        val farmClaims = claims.filter { it.key !in meowfiaSet }
        val claimedFarmRoles = farmClaims.values.map { it.claimedRole }

        val hasTwinflower = RoleId.TWINFLOWER in pool
        if (!hasTwinflower) {
            val nonBufferCounts = claimedFarmRoles.filter { !it.isBuffer }.groupingBy { it }.eachCount()
            val poolCounts = pool.filter { !it.isFlower }.groupingBy { it }.eachCount()
            for ((role, count) in nonBufferCounts) {
                val available = (poolCounts[role] ?: 0) + if (role.isBuffer) Int.MAX_VALUE else 0
                if (count > maxOf(1, available)) return false
            }
        }

        if (meowfiaSet.isEmpty()) {
            // No Meowfia — just simulate directly
            return simulationMatches(meowfiaSet, emptyMap(), emptyMap(), farmClaims, claims, pool, playerIds)
        }

        // Step 2: Collect candidate Meowfia roles from pool
        val meowfiaRolesInPool = pool.filter { it.isMeowfiaAnimal }
            .ifEmpty { listOf(RoleId.HOUSE_CAT) } // always have the buffer

        // Step 3: Enumerate role assignments for the Meowfia set
        val meowfiaIds = meowfiaSet.toList()

        for (roleAssignment in assignMeowfiaRoles(meowfiaIds, meowfiaRolesInPool)) {
            // Build hypothetical player list for this role assignment so
            // getValidTargets can inspect alignments and roles
            val hypotheticalPlayers = playerIds.map { id ->
                val isMeowfia = id in meowfiaSet
                val claim = claims[id]!!
                Player(
                    id = id,
                    name = "P$id",
                    alignment = if (isMeowfia) Alignment.MEOWFIA else Alignment.FARM,
                    roleId = if (isMeowfia) (roleAssignment[id] ?: RoleId.HOUSE_CAT) else claim.claimedRole
                )
            }

            // Step 4: Enumerate visit targets for each Meowfia player
            for (visitAssignment in assignVisitTargets(meowfiaIds, hypotheticalPlayers, roleAssignment)) {
                if (simulationMatches(meowfiaSet, roleAssignment, visitAssignment, farmClaims, claims, pool, playerIds)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Run the NightResolver simulation and check if computed Farm egg deltas
     * match what Farm players claimed.
     */
    private fun simulationMatches(
        meowfiaSet: Set<Int>,
        roleAssignment: Map<Int, RoleId>,
        visitAssignment: Map<Int, Int>,
        farmClaims: Map<Int, ClaimData>,
        allClaims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        playerIds: List<Int>
    ): Boolean {
        val players = playerIds.map { id ->
            val isMeowfia = id in meowfiaSet
            val claim = allClaims[id]!!
            Player(
                id = id,
                name = "P$id",
                alignment = if (isMeowfia) Alignment.MEOWFIA else Alignment.FARM,
                roleId = if (isMeowfia) (roleAssignment[id] ?: RoleId.HOUSE_CAT) else claim.claimedRole
            )
        }

        // Build visit graph: Farm uses claimed targets, Meowfia uses enumerated targets
        val simVisitGraph = mutableMapOf<Int, Int?>()
        for (id in playerIds) {
            if (id in meowfiaSet) {
                simVisitGraph[id] = visitAssignment[id]
            } else {
                simVisitGraph[id] = allClaims[id]!!.claimedTargetId
            }
        }

        // Build night actions — use explicit VisitPlayer for all known targets
        // so the resolver doesn't roll random targets that differ from reality
        val nightActions = playerIds.associateWith { id ->
            val target = simVisitGraph[id]
            val claim = allClaims[id]!!
            when {
                target != null -> NightAction.VisitPlayer(target)
                claim.claimedRole == RoleId.BLACK_SWAN -> NightAction.VisitSelf
                claim.claimedRole == RoleId.TURKEY -> NightAction.NoVisit
                id in meowfiaSet -> NightAction.NoVisit // Meowfia with no target
                else -> NightAction.NoVisit
            }
        }

        val gameState = GameState(
            roundNumber = 1,
            players = players,
            pool = pool.map { PoolCard(it) },
            dealerSeat = 0,
            nightActions = nightActions,
            nightResults = emptyMap(),
            dawnReports = emptyMap(),
            activeFlowers = emptyList(),
            visitGraph = simVisitGraph,
            phase = GamePhase.NIGHT,
            cawCawCount = 0,
            eliminatedPlayerId = null
        )

        val resolver = NightResolver(RandomProvider(0))
        val context = resolver.resolve(gameState)

        for ((id, claim) in farmClaims) {
            val computedDelta = context.getClampedEggDelta(id)
            if (computedDelta != claim.claimedEggDelta) return false
        }
        return true
    }

    /**
     * Generate every possible assignment of Meowfia roles to the Meowfia players.
     * Each player gets one role from the pool (with replacement for HOUSE_CAT buffer).
     */
    private fun assignMeowfiaRoles(
        meowfiaIds: List<Int>,
        candidateRoles: List<RoleId>
    ): List<Map<Int, RoleId>> {
        val distinct = candidateRoles.distinct()
        val results = mutableListOf<Map<Int, RoleId>>()

        fun recurse(index: Int, current: Map<Int, RoleId>, usedNonBuffer: Set<RoleId>) {
            if (index == meowfiaIds.size) {
                results.add(current)
                return
            }
            for (role in distinct) {
                // Non-buffer roles can only be assigned once
                if (!role.isBuffer && role in usedNonBuffer) continue
                val newUsed = if (role.isBuffer) usedNonBuffer else usedNonBuffer + role
                recurse(index + 1, current + (meowfiaIds[index] to role), newUsed)
            }
        }

        recurse(0, emptyMap(), emptySet())
        return results
    }

    /**
     * Generate every possible visit-target assignment for Meowfia players.
     * Uses [RoleRegistry] to query each role's [RoleHandler.getValidTargets],
     * so targeting rules are defined once in the handler and shared with the solver.
     */
    private fun assignVisitTargets(
        meowfiaIds: List<Int>,
        allPlayers: List<Player>,
        roleAssignment: Map<Int, RoleId>
    ): List<Map<Int, Int>> {
        val results = mutableListOf<Map<Int, Int>>()

        fun validTargetsFor(playerId: Int): List<Int> {
            val role = roleAssignment[playerId] ?: RoleId.HOUSE_CAT
            val handler = RoleRegistry.get(role)
            val actor = allPlayers.find { it.id == playerId } ?: return emptyList()
            return handler.getValidTargets(actor, allPlayers).map { it.id }
        }

        fun recurse(index: Int, current: Map<Int, Int>) {
            if (index == meowfiaIds.size) {
                results.add(current)
                return
            }
            val id = meowfiaIds[index]
            val targets = validTargetsFor(id)
            if (targets.isEmpty()) {
                recurse(index + 1, current)
                return
            }
            for (target in targets) {
                recurse(index + 1, current + (id to target))
            }
        }

        recurse(0, emptyMap())
        return results
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
