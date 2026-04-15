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
        /** Players can take the correct action: either all worlds agree there
         *  are 0 Meowfia (vote nobody), or the 0-Meowfia world is inconsistent
         *  and at least one player is Meowfia in every consistent world. */
        ACTIONABLE,
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
        /** Player IDs that are Meowfia in EVERY consistent world.
         *  If non-empty, Farm can safely eliminate any of these players. */
        val guaranteedMeowfia: Set<Int> = emptySet(),
        /** Number of consistent Meowfia subsets found. */
        val consistentWorlds: Int,
        /** Total candidate subsets evaluated. */
        val totalCandidates: Int,
        /** Human-readable reasons for the deduction. */
        val reasons: List<String>,
        val playerCount: Int,
        val meowfiaCount: Int,
        /** Per-player suspicion: how often they appear as Meowfia across consistent worlds (0.0–1.0). */
        val suspicionRatings: Map<Int, Float> = emptyMap(),
        /** Each consistent world as a set of Meowfia player IDs. */
        val consistentWorldDetails: List<Set<Int>> = emptyList()
    ) {
        /** Human-readable verdict label with suspect count detail. */
        val verdictLabel: String get() {
            val zeroMeowfiaConsistent = consistentWorldDetails.any { it.isEmpty() }
            return when (solvability) {
                Solvability.SOLVED -> "SOLVED"
                Solvability.ACTIONABLE -> "ACTIONABLE"
                Solvability.NARROWED -> {
                    val suffix = if (zeroMeowfiaConsistent) " OR NONE" else ""
                    when {
                        suspects.isEmpty() -> "NO SUSPECTS"
                        suspects.size == 1 -> "1 SUSPECT$suffix"
                        else -> "${suspects.size} SUSPECTS$suffix"
                    }
                }
                Solvability.COIN_FLIP -> "COULD BE ANYONE"
            }
        }
    }

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
        visitGraph: Map<Int, Int?>,
        actualVisitGraph: Map<Int, Int?> = visitGraph,
        playerNames: Map<Int, String> = emptyMap(),
        /** If true, try all possible Meowfia visit targets (slower but more accurate). */
        exhaustiveSearch: Boolean = false
    ): SolvabilityResult {
        val playerIds = claims.keys.sorted()
        val playerCount = playerIds.size
        val meowfiaCount = assignments.count { it.alignment == Alignment.MEOWFIA }
        val names = playerNames.ifEmpty { playerIds.associateWith { "Player $it" } }
        val reasons = mutableListOf<String>()
        useExhaustiveSearch = exhaustiveSearch

        // Quick pre-check: role claim conflicts
        val claimConflicts = findClaimConflicts(claims, pool)
        reasons.addAll(claimConflicts)

        // Enumerate all possible Meowfia subsets of size 0..playerCount
        // For typical games (4-8 players), this is at most 2^8 = 256 subsets
        val consistentMeowfiaPlayers = mutableSetOf<Int>()
        val consistentWorldList = mutableListOf<Set<Int>>()
        var totalCandidates = 0

        // We check Farm-minority worlds (Meowfia < half) for the main analysis.
        // These are the worlds where deduction matters — Farm is trying to find cats.
        val maxMeowfiaSize = (playerCount - 1) / 2
        for (subsetSize in 0..maxMeowfiaSize) {
            for (meowfiaSubset in combinations(playerIds, subsetSize)) {
                totalCandidates++
                if (isConsistent(meowfiaSubset.toSet(), claims, pool, visitGraph, playerIds, dawnReports, actualVisitGraph)) {
                    consistentWorldList.add(meowfiaSubset.toSet())
                    consistentMeowfiaPlayers.addAll(meowfiaSubset)
                }
            }
        }
        val consistentWorlds = consistentWorldList.size

        // Compute per-player suspicion rating: frequency of appearing as Meowfia
        val suspicionRatings = if (consistentWorlds > 0) {
            playerIds.associateWith { pid ->
                consistentWorldList.count { pid in it }.toFloat() / consistentWorlds
            }
        } else emptyMap()

        // Also run investigation cross-reference checks
        val investigationReasons = checkInvestigations(claims, dawnReports)
        reasons.addAll(investigationReasons)

        // Bonus: check if Meowfia players' own deltas are plausible.
        // Even as Meowfia (House Cat, no egg action), they receive eggs from visitors.
        // Count how many Farm players claim to visit each player with egg-laying roles.
        for (pid in playerIds) {
            if (pid !in consistentMeowfiaPlayers) continue // only check suspects
            val claim = claims[pid] ?: continue
            val farmVisitorsLaying = claims.entries.count { (otherId, otherClaim) ->
                otherId != pid &&
                otherClaim.claimedTargetId == pid &&
                otherClaim.claimedRole in setOf(RoleId.PIGEON, RoleId.MOSQUITO, RoleId.TIT, RoleId.CHICKEN, RoleId.FALCON)
            }
            var maxEggsFromFarm = 0
            for ((otherId, otherClaim) in claims) {
                if (otherId != pid && otherClaim.claimedTargetId == pid) {
                    maxEggsFromFarm += when (otherClaim.claimedRole) {
                        RoleId.CHICKEN -> 2
                        RoleId.PIGEON, RoleId.MOSQUITO, RoleId.TIT, RoleId.FALCON -> 1
                        else -> 0
                    }
                }
            }
            if (claim.claimedEggDelta > maxEggsFromFarm + maxMeowfiaSize) {
                val pName = names[pid] ?: "Player $pid"
                reasons.add("$pName claims ${claim.claimedEggDelta} eggs, but even as Meowfia only $maxEggsFromFarm eggs could be received from claimed Farm visitors")
            }
        }

        // Determine suspects and cleared
        var suspects: Set<Int>
        var cleared: Set<Int>

        if (consistentWorlds == 0) {
            // No world is consistent. Do per-player analysis: for each player,
            // check if making ONLY them Meowfia produces a consistent world.
            // Players who can never be Farm (their Farm-worlds always fail) are suspects.
            val perPlayerFarmFails = mutableMapOf<Int, Int>()
            val perPlayerFarmTotal = mutableMapOf<Int, Int>()
            for (world in consistentWorldList) {
                // already empty, but let's recompute per-player
            }
            // Re-check: for each player, how many worlds where they're Farm are inconsistent?
            for (pid in playerIds) {
                var farmWorlds = 0
                var farmConsistent = 0
                // Check all size-1 worlds where this player is NOT Meowfia
                for (otherPid in playerIds) {
                    if (otherPid == pid) continue
                    val testWorld = setOf(otherPid)
                    if (testWorld.size * 2 < playerCount) { // Farm-majority check
                        farmWorlds++
                        if (isConsistent(testWorld, claims, pool, visitGraph, playerIds, dawnReports, actualVisitGraph)) {
                            farmConsistent++
                        }
                    }
                }
                // Also check the zero-Meowfia world
                farmWorlds++
                if (isConsistent(emptySet(), claims, pool, visitGraph, playerIds, dawnReports, actualVisitGraph)) {
                    farmConsistent++
                }
                perPlayerFarmFails[pid] = farmWorlds - farmConsistent
                perPlayerFarmTotal[pid] = farmWorlds
            }

            // Players who ALWAYS fail when assumed Farm are the prime suspects
            val alwaysFail = playerIds.filter { pid ->
                val total = perPlayerFarmTotal[pid] ?: 0
                val fails = perPlayerFarmFails[pid] ?: 0
                total > 0 && fails == total
            }.toSet()

            if (alwaysFail.isNotEmpty() && alwaysFail.size < playerIds.size) {
                suspects = alwaysFail
                cleared = playerIds.filter { it !in suspects }.toSet()
                reasons.add("${suspects.size} player(s) can never be Farm consistently — they must be lying")
            } else {
                // Truly contradictory — everyone is suspect
                suspects = playerIds.toSet()
                cleared = emptySet()
            }
        } else {
            suspects = consistentMeowfiaPlayers
            cleared = playerIds.filter { it !in suspects }.toSet()
        }

        // Classify based on how many worlds were eliminated
        val eliminatedPct = if (totalCandidates > 0)
            (1.0 - consistentWorlds.toDouble() / totalCandidates) * 100 else 100.0

        // SOLVED requires all consistent worlds to agree on the exact same Meowfia set.
        val allWorldsAgree = consistentWorldList.isNotEmpty() &&
            consistentWorldList.distinct().size == 1

        // Guaranteed Meowfia: players who are Meowfia in EVERY consistent world.
        // If the 0-Meowfia world is consistent, no one can be guaranteed Meowfia.
        val zeroMeowfiaConsistent = consistentWorldList.any { it.isEmpty() }
        val nonEmptyWorlds = consistentWorldList.filter { it.isNotEmpty() }
        val guaranteedMeowfia = if (consistentWorldList.isNotEmpty() && !zeroMeowfiaConsistent && nonEmptyWorlds.isNotEmpty()) {
            // A player is guaranteed Meowfia if they appear in every consistent world
            playerIds.filter { pid -> consistentWorldList.all { pid in it } }.toSet()
        } else {
            emptySet()
        }

        // ACTIONABLE means players can take the correct action:
        // - All consistent worlds are 0-Meowfia → vote nobody
        // - OR the 0-Meowfia world is impossible AND at least one player is
        //   guaranteed Meowfia → eliminate them
        val allWorldsZeroMeowfia = consistentWorldList.isNotEmpty() &&
            consistentWorldList.all { it.isEmpty() }
        val canAct = allWorldsZeroMeowfia || guaranteedMeowfia.isNotEmpty()

        val solvability = when {
            consistentWorlds == 0 -> {
                if (suspects.size <= meowfiaCount + 1 && suspects.size < playerIds.size) {
                    reasons.add("Claims are contradictory — but the liars can be identified")
                    Solvability.SOLVED
                } else {
                    reasons.add("Claims are contradictory — someone is lying but can't pinpoint exactly who")
                    Solvability.NARROWED
                }
            }
            allWorldsAgree -> {
                reasons.add("All consistent worlds agree on the same Meowfia set")
                Solvability.SOLVED
            }
            canAct -> {
                if (allWorldsZeroMeowfia) {
                    reasons.add("All consistent worlds have 0 Meowfia — safe to vote nobody")
                } else {
                    val names = guaranteedMeowfia.mapNotNull { id -> names[id] }
                    reasons.add("${names.joinToString(", ")} must be Meowfia in every consistent world — safe to eliminate")
                }
                Solvability.ACTIONABLE
            }
            eliminatedPct > 0 -> {
                reasons.add("${cleared.size} player(s) cleared, ${suspects.size} remain as suspects")
                Solvability.NARROWED
            }
            else -> Solvability.COIN_FLIP
        }

        // Generate detailed per-player reasoning
        for (pid in playerIds) {
            val claim = claims[pid] ?: continue
            val report = dawnReports.find { it.playerId == pid }
            val pName = names[pid] ?: "Player $pid"

            if (pid in cleared) {
                reasons.add("${pName} is cleared — their claim (${claim.claimedRole.displayName}, ${claim.claimedEggDelta} eggs) is consistent in at least one scenario")
            } else if (pid in suspects) {
                // Find the specific reason by checking what fails when this player is Farm
                val testWorld = setOf(pid)  // everyone else is Farm, only this player is Meowfia
                // Actually check the OTHER way: what happens when this player is assumed Farm?
                // They're a suspect because every world where they're Farm fails
                val failExample = playerIds.filter { it != pid }.firstNotNullOfOrNull { otherId ->
                    checkConsistency(setOf(otherId), claims, pool, playerIds, names, actualVisitGraph = actualVisitGraph)
                        ?.takeIf { true } // find first world where this player is Farm that fails
                } ?: run {
                    // Check zero-Meowfia world with this player as Farm
                    checkConsistency(emptySet(), claims, pool, playerIds, names, actualVisitGraph = actualVisitGraph)
                }
                val failReason = failExample ?: "appears as Meowfia in all consistent scenarios"
                reasons.add("${pName} is suspicious — $failReason")
            }
        }

        return SolvabilityResult(
            solvability = solvability,
            suspects = suspects,
            cleared = cleared,
            guaranteedMeowfia = guaranteedMeowfia,
            consistentWorlds = consistentWorlds,
            totalCandidates = totalCandidates,
            reasons = reasons,
            playerCount = playerCount,
            meowfiaCount = meowfiaCount,
            suspicionRatings = suspicionRatings,
            consistentWorldDetails = consistentWorldList
        )
    }

    /**
     * Check if a hypothetical Meowfia subset is consistent using ONLY claims.
     * Builds a hypothetical game from what assumed-Farm players claim (their roles,
     * targets, etc.), simulates it with the real engine, then checks if the
     * simulated egg deltas match what each player CLAIMS their delta was.
     *
     * Returns null if consistent, or a failure reason if not.
     *
     * When called from the exhaustive path, [meowfiaRoleOverrides] provides
     * the hypothesized Meowfia roles and [actualVisitGraph] resolves random
     * Farm targets (Mosquito/Tit) deterministically.
     */
    /**
     * @param meowfiaRoleOverrides If provided, overrides the role for Meowfia
     *   players instead of always using House Cat.
     * @param actualVisitGraph Real visit graph — used to resolve Farm Mosquito/Tit
     *   targets deterministically instead of relying on VisitRandom + seed.
     */
    private fun checkConsistency(
        meowfiaSet: Set<Int>,
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        playerIds: List<Int>,
        playerNames: Map<Int, String> = emptyMap(),
        meowfiaRoleOverrides: Map<Int, RoleId> = emptyMap(),
        actualVisitGraph: Map<Int, Int?> = emptyMap()
    ): String? {
        val farmClaims = claims.filter { it.key !in meowfiaSet }
        fun name(id: Int) = playerNames[id] ?: "Player $id"

        // Step 1: Role feasibility
        val hasTwinflower = RoleId.TWINFLOWER in pool
        if (!hasTwinflower) {
            val claimedFarmRoles = farmClaims.values.map { it.claimedRole }
            val nonBufferCounts = claimedFarmRoles.filter { !it.isBuffer }.groupingBy { it }.eachCount()
            val poolRoles = pool.filter { !it.isFlower }.toSet() + setOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
            for ((role, count) in nonBufferCounts) {
                if (!role.isBuffer && role !in poolRoles) {
                    val who = farmClaims.entries.first { it.value.claimedRole == role }
                    return "${name(who.key)} claims ${role.displayName} which is not in the pool"
                }
                if (!role.isBuffer && count > 1) {
                    val who = farmClaims.filter { it.value.claimedRole == role }.keys.map { name(it) }
                    return "${who.joinToString(" and ")} both claim ${role.displayName} but only one can have it"
                }
            }
        }

        // Step 2: Simulate the round using claims as ground truth
        val players = playerIds.map { id ->
            val isMeowfia = id in meowfiaSet
            val claim = claims[id]!!
            Player(
                id = id, name = name(id),
                alignment = if (isMeowfia) Alignment.MEOWFIA else Alignment.FARM,
                roleId = if (isMeowfia) (meowfiaRoleOverrides[id] ?: RoleId.HOUSE_CAT) else claim.claimedRole
            )
        }

        // Build visit graph: use claim target, fall back to actual visit graph for
        // roles with random targets (Mosquito, Tit) so we don't depend on seed
        val simVisitGraph = mutableMapOf<Int, Int?>()
        for (id in playerIds) {
            val claimedTarget = claims[id]!!.claimedTargetId
            simVisitGraph[id] = claimedTarget ?: actualVisitGraph[id]
        }

        // Build night actions — always use explicit VisitPlayer when target is known
        val nightActions = playerIds.associateWith { id ->
            val target = simVisitGraph[id]
            val claim = claims[id]!!
            when {
                target != null -> NightAction.VisitPlayer(target)
                claim.claimedRole == RoleId.BLACK_SWAN -> NightAction.VisitSelf
                claim.claimedRole == RoleId.TURKEY -> NightAction.NoVisit
                else -> NightAction.NoVisit
            }
        }

        val gameState = GameState(
            roundNumber = 1, players = players,
            pool = pool.map { PoolCard(it) }, dealerSeat = 0,
            nightActions = nightActions,
            nightResults = emptyMap(), dawnReports = emptyMap(),
            activeFlowers = pool.filter { it.isFlower },
            visitGraph = simVisitGraph, phase = GamePhase.NIGHT,
            cawCawCount = 0, eliminatedPlayerId = null
        )

        val resolver = NightResolver(RandomProvider(0))
        val context = resolver.resolve(gameState)

        // Step 3: Check simulated deltas against CLAIMED deltas
        for ((id, claim) in farmClaims) {
            val simulated = context.getClampedEggDelta(id)
            if (simulated != claim.claimedEggDelta) {
                return "${name(id)} claims ${claim.claimedEggDelta} eggs as ${claim.claimedRole.displayName}, but the math gives $simulated"
            }
        }

        return null
    }

    private var useExhaustiveSearch = false

    private fun isConsistent(
        meowfiaSet: Set<Int>,
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        visitGraph: Map<Int, Int?>,
        playerIds: List<Int>,
        dawnReports: List<DawnReport>,
        actualVisitGraph: Map<Int, Int?>
    ): Boolean {
        if (meowfiaSet.isEmpty()) {
            return checkConsistency(meowfiaSet, claims, pool, playerIds, actualVisitGraph = actualVisitGraph) == null
        }

        if (!useExhaustiveSearch) {
            // Fast path: use claimed targets as-is (good enough for batch simulations)
            return checkConsistency(meowfiaSet, claims, pool, playerIds, actualVisitGraph = actualVisitGraph) == null
        }

        // Exhaustive: enumerate Meowfia role assignments × valid visit targets
        val meowfiaIds = meowfiaSet.toList()
        val meowfiaRolesInPool = pool.filter { it.isMeowfiaAnimal }.distinct()
            .ifEmpty { listOf(RoleId.HOUSE_CAT) }

        for (roleAssignment in assignMeowfiaRoles(meowfiaIds, meowfiaRolesInPool)) {
            // Build hypothetical players for this role assignment so
            // getValidTargets can inspect alignments and roles
            val hypotheticalPlayers = playerIds.map { id ->
                val isMeowfia = id in meowfiaSet
                val claim = claims[id]!!
                Player(
                    id = id, name = "P$id",
                    alignment = if (isMeowfia) Alignment.MEOWFIA else Alignment.FARM,
                    roleId = if (isMeowfia) (roleAssignment[id] ?: RoleId.HOUSE_CAT) else claim.claimedRole
                )
            }

            if (tryMeowfiaVisits(
                meowfiaIds, 0, claims.toMutableMap(),
                hypotheticalPlayers, meowfiaSet, pool, playerIds,
                roleAssignment, actualVisitGraph
            )) return true
        }
        return false
    }

    /**
     * Generate every possible assignment of Meowfia roles to the Meowfia players.
     * Non-buffer roles can only be assigned once; House Cat (buffer) is reusable.
     */
    private fun assignMeowfiaRoles(
        meowfiaIds: List<Int>,
        candidateRoles: List<RoleId>
    ): List<Map<Int, RoleId>> {
        val results = mutableListOf<Map<Int, RoleId>>()

        fun recurse(index: Int, current: Map<Int, RoleId>, usedNonBuffer: Set<RoleId>) {
            if (index == meowfiaIds.size) {
                results.add(current)
                return
            }
            for (role in candidateRoles) {
                if (!role.isBuffer && role in usedNonBuffer) continue
                val newUsed = if (role.isBuffer) usedNonBuffer else usedNonBuffer + role
                recurse(index + 1, current + (meowfiaIds[index] to role), newUsed)
            }
        }

        recurse(0, emptyMap(), emptySet())
        return results
    }

    /**
     * Recursively try all valid visit targets for Meowfia players.
     * Uses [RoleRegistry] to query each role's valid targets.
     */
    private fun tryMeowfiaVisits(
        meowfiaIds: List<Int>,
        index: Int,
        claims: MutableMap<Int, ClaimData>,
        hypotheticalPlayers: List<Player>,
        meowfiaSet: Set<Int>,
        pool: List<RoleId>,
        playerIds: List<Int>,
        roleAssignment: Map<Int, RoleId>,
        actualVisitGraph: Map<Int, Int?>
    ): Boolean {
        if (index >= meowfiaIds.size) {
            return checkConsistency(
                meowfiaSet, claims, pool, playerIds,
                meowfiaRoleOverrides = roleAssignment,
                actualVisitGraph = actualVisitGraph
            ) == null
        }

        val meowfiaId = meowfiaIds[index]
        val originalClaim = claims[meowfiaId]!!
        val role = roleAssignment[meowfiaId] ?: RoleId.HOUSE_CAT
        val actor = hypotheticalPlayers.find { it.id == meowfiaId }!!

        // Use RoleRegistry to get valid targets for this role
        val validTargets = if (RoleRegistry.isRegistered(role)) {
            RoleRegistry.get(role).getValidTargets(actor, hypotheticalPlayers).map { it.id }
        } else {
            // Fallback: all other players
            playerIds.filter { it != meowfiaId }
        }

        for (target in validTargets) {
            claims[meowfiaId] = originalClaim.copy(claimedTargetId = target)
            if (tryMeowfiaVisits(meowfiaIds, index + 1, claims, hypotheticalPlayers, meowfiaSet, pool, playerIds, roleAssignment, actualVisitGraph)) {
                claims[meowfiaId] = originalClaim
                return true
            }
        }

        // Also try no-visit (e.g. if valid targets is empty, or role stays home)
        if (validTargets.isEmpty()) {
            claims[meowfiaId] = originalClaim.copy(claimedTargetId = null)
            if (tryMeowfiaVisits(meowfiaIds, index + 1, claims, hypotheticalPlayers, meowfiaSet, pool, playerIds, roleAssignment, actualVisitGraph)) {
                claims[meowfiaId] = originalClaim
                return true
            }
        }

        claims[meowfiaId] = originalClaim
        return false
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
