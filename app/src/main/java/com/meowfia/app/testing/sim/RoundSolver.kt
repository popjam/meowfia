package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.RoleId

/**
 * Analyzes a completed round to determine if Meowfia players are deducible
 * from public information (claims, egg deltas, pool composition, investigation results).
 *
 * Works from the Farm perspective: given what everyone claimed, can we figure out who's lying?
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
        /** Player IDs flagged as suspicious (empty for COIN_FLIP). */
        val suspects: Set<Int>,
        /** Player IDs cleared as definitely Farm (empty if none can be cleared). */
        val cleared: Set<Int>,
        /** Human-readable reasons for the deduction. */
        val reasons: List<String>,
        /** Total player count (for context). */
        val playerCount: Int,
        /** Actual Meowfia count (for validation). */
        val meowfiaCount: Int
    )

    /**
     * Analyze a round's public information to determine solvability.
     *
     * @param claims Each player's day claim (role, target, egg delta). Indexed by player ID.
     * @param pool The revealed pool of roles for this round.
     * @param dawnReports Each player's dawn report (for investigation info).
     * @param assignments Ground truth assignments (used to validate, not for solving).
     * @param visitGraph Who visited whom (public via claims, not the real graph).
     */
    fun analyze(
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        dawnReports: List<DawnReport>,
        assignments: List<PlayerAssignment>,
        visitGraph: Map<Int, Int?>
    ): SolvabilityResult {
        val playerIds = claims.keys.toSet()
        val playerCount = playerIds.size
        val meowfiaCount = assignments.count { it.alignment == Alignment.MEOWFIA }
        val reasons = mutableListOf<String>()
        val suspects = mutableSetOf<Int>()
        val cleared = mutableSetOf<Int>()

        // --- Check 1: Role claim conflicts ---
        checkRoleConflicts(claims, pool, suspects, cleared, reasons)

        // --- Check 2: Egg accounting ---
        checkEggAccounting(claims, pool, playerCount, suspects, reasons)

        // --- Check 3: Investigation cross-reference ---
        checkInvestigations(claims, dawnReports, assignments, suspects, cleared, reasons)

        // --- Check 4: Target consistency ---
        checkTargetConsistency(claims, suspects, reasons)

        // Classify result
        val solvability = when {
            // If we've identified exactly the right number of suspects
            suspects.size in 1..meowfiaCount && suspects.isNotEmpty() -> Solvability.SOLVED
            // If cleared players narrow the field
            cleared.size > 0 && (playerCount - cleared.size) <= meowfiaCount + 1 -> Solvability.SOLVED
            // If we have some suspects or cleared players
            suspects.isNotEmpty() || cleared.size > playerCount / 2 -> Solvability.NARROWED
            else -> Solvability.COIN_FLIP
        }

        return SolvabilityResult(
            solvability = solvability,
            suspects = suspects,
            cleared = cleared,
            reasons = reasons,
            playerCount = playerCount,
            meowfiaCount = meowfiaCount
        )
    }

    /**
     * Check 1: Do multiple players claim the same non-buffer role?
     * Is anyone claiming a role not in the pool?
     */
    private fun checkRoleConflicts(
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        suspects: MutableSet<Int>,
        cleared: MutableSet<Int>,
        reasons: MutableList<String>
    ) {
        val poolRoles = pool.filter { !it.isFlower }.map { it }.toMutableList()
        // Buffers (Pigeon, House Cat) are always available as overflow
        poolRoles.add(RoleId.PIGEON)
        poolRoles.add(RoleId.HOUSE_CAT)

        // Check for roles claimed that aren't in the pool at all
        for ((playerId, claim) in claims) {
            if (claim.claimedRole !in poolRoles && !claim.claimedRole.isBuffer) {
                suspects.add(playerId)
                reasons.add("Player $playerId claims ${claim.claimedRole.displayName} which isn't in the pool")
            }
        }

        // Check for duplicate non-buffer claims
        val roleClaims = claims.entries
            .groupBy { it.value.claimedRole }
            .filter { (role, claimants) -> !role.isBuffer && claimants.size > 1 }

        for ((role, claimants) in roleClaims) {
            val ids = claimants.map { it.key }
            // Pool has limited copies — count how many of this role are in pool
            val poolCount = pool.count { it == role }
            if (ids.size > maxOf(1, poolCount)) {
                suspects.addAll(ids)
                reasons.add("${ids.size} players claim ${role.displayName} but pool only has $poolCount")
            }
        }
    }

    /**
     * Check 2: Do the total eggs claimed add up given the pool composition?
     * Each egg-laying role produces a known amount — if the total claimed received
     * eggs exceed what the pool could produce, someone's lying about their delta.
     */
    private fun checkEggAccounting(
        claims: Map<Int, ClaimData>,
        pool: List<RoleId>,
        playerCount: Int,
        suspects: MutableSet<Int>,
        reasons: MutableList<String>
    ) {
        // Maximum eggs the pool could produce in one night
        val maxPoolEggs = pool.sumOf { role ->
            when (role) {
                RoleId.CHICKEN -> 2
                RoleId.PIGEON, RoleId.MOSQUITO, RoleId.TIT, RoleId.FALCON -> 1
                RoleId.OWL -> 1  // Owl lays 1 if no visitors on target
                RoleId.EAGLE -> playerCount - 1  // Theoretical max (all visitors on one target)
                RoleId.HAWK -> 1  // Hawk gains 1 if target is Meowfia
                RoleId.BLACK_SWAN -> 1
                else -> 0
            }
        }
        // Add Turkey eggs: each visitor to Turkey gets 1 egg
        val hasTurkey = pool.contains(RoleId.TURKEY)
        val turkeyMax = if (hasTurkey) playerCount - 1 else 0

        val totalMaxEggs = maxPoolEggs + turkeyMax

        // Sum of all claimed positive deltas
        val totalClaimedPositive = claims.values.sumOf { maxOf(0, it.claimedEggDelta) }

        if (totalClaimedPositive > totalMaxEggs) {
            reasons.add("Total claimed positive eggs ($totalClaimedPositive) exceeds pool maximum ($totalMaxEggs)")
            // Flag players with highest deltas as most suspicious
            val sorted = claims.entries.sortedByDescending { it.value.claimedEggDelta }
            val excess = totalClaimedPositive - totalMaxEggs
            var remaining = excess
            for ((playerId, claim) in sorted) {
                if (remaining <= 0) break
                if (claim.claimedEggDelta > 0) {
                    suspects.add(playerId)
                    remaining -= claim.claimedEggDelta
                }
            }
        }

        // Per-player egg verification: if someone claims to have received eggs
        // from a role that claims to have visited someone ELSE, that's a conflict
        val layerVisits = mutableMapOf<Int, MutableList<Pair<RoleId, Int>>>() // targetId -> [(role, eggs)]
        for ((playerId, claim) in claims) {
            val eggsLaid = when (claim.claimedRole) {
                RoleId.PIGEON -> 1
                RoleId.CHICKEN -> 2
                RoleId.MOSQUITO -> 1
                RoleId.TIT -> 1
                RoleId.FALCON -> 1  // Indirect, harder to track
                RoleId.OWL -> 1  // Only if no visitors
                else -> 0
            }
            if (eggsLaid > 0 && claim.claimedTargetId != null) {
                layerVisits.getOrPut(claim.claimedTargetId) { mutableListOf() }
                    .add(claim.claimedRole to eggsLaid)
            }
        }

        // Check: does each player's claimed delta at least roughly match incoming eggs?
        for ((playerId, claim) in claims) {
            val incomingEggs = layerVisits[playerId]?.sumOf { it.second } ?: 0
            val selfEggs = when (claim.claimedRole) {
                RoleId.HAWK -> if (claim.claimedEggDelta > incomingEggs) 1 else 0
                RoleId.BLACK_SWAN -> if (claim.claimedEggDelta > incomingEggs) 1 else 0
                RoleId.EAGLE -> maxOf(0, claim.claimedEggDelta - incomingEggs)
                else -> 0
            }
            val expectedMax = incomingEggs + selfEggs + 1  // +1 for Turkey/untracked
            if (claim.claimedEggDelta > expectedMax + 1) {
                suspects.add(playerId)
                reasons.add("Player $playerId claims ${claim.claimedEggDelta} eggs but max accountable is ~$expectedMax")
            }
        }
    }

    /**
     * Check 3: Cross-reference investigation results.
     * - Hawk with +1 egg → their target should be Meowfia
     * - Owl info about visitor animals → should match who claims to have visited
     */
    private fun checkInvestigations(
        claims: Map<Int, ClaimData>,
        dawnReports: List<DawnReport>,
        assignments: List<PlayerAssignment>,
        suspects: MutableSet<Int>,
        cleared: MutableSet<Int>,
        reasons: MutableList<String>
    ) {
        for ((playerId, claim) in claims) {
            val report = dawnReports.find { it.playerId == playerId } ?: continue

            when (claim.claimedRole) {
                RoleId.HAWK -> {
                    // If Hawk claims +1 from investigation (not just from visitors),
                    // and we can verify via dawn report, their target is Meowfia
                    if (claim.claimedEggDelta > 0 && claim.claimedTargetId != null) {
                        // This is info the Hawk has — they know their target's alignment
                        // Other players can use this if they trust the Hawk claim
                        val targetAssignment = assignments.find { it.playerId == claim.claimedTargetId }
                        if (targetAssignment != null && report.additionalInfo.isEmpty()) {
                            // Hawk doesn't get dawn info — egg delta is the signal
                            // If a trusted Hawk says they got an egg, target is suspicious
                            // But we can't distinguish Hawk's self-egg from visitor eggs easily
                        }
                    }
                }
                RoleId.OWL -> {
                    // Owl's dawn info lists visitor animals
                    // If Owl says "Pigeon visited X" but nobody claims Pigeon visiting X,
                    // either the Owl or the Pigeon is lying
                    for (info in report.additionalInfo) {
                        if (info.contains("Animals that visited")) {
                            val animalNames = info.substringAfter(": ").removeSuffix(".")
                                .split(", ")
                            val targetId = claim.claimedTargetId ?: continue

                            // Check if claimed visitors match who says they visited this target
                            val claimedVisitors = claims.entries
                                .filter { it.value.claimedTargetId == targetId && it.key != playerId }
                                .map { it.value.claimedRole.displayName }

                            for (animal in animalNames) {
                                if (animal !in claimedVisitors) {
                                    reasons.add("Owl reports $animal visited player $targetId but no one claims that role visiting them")
                                }
                            }
                            for (visitor in claimedVisitors) {
                                if (visitor !in animalNames && visitor != "Owl") {
                                    reasons.add("Player claims ${visitor} visited $targetId but Owl didn't see them")
                                }
                            }
                        }
                        if (info.contains("No animals visited")) {
                            val targetId = claim.claimedTargetId ?: continue
                            val visitorsToTarget = claims.entries
                                .filter { it.value.claimedTargetId == targetId && it.key != playerId }
                            if (visitorsToTarget.isNotEmpty()) {
                                val conflicting = visitorsToTarget.map { it.key }
                                suspects.addAll(conflicting)
                                reasons.add("Owl says nobody visited player $targetId but ${conflicting.size} player(s) claim they did")
                            }
                        }
                    }
                }
                else -> { /* No investigation info to cross-reference */ }
            }
        }

        // Players whose claims are fully consistent with all other evidence get cleared
        // (simplified: players not in suspects with consistent claims)
        for (playerId in claims.keys) {
            if (playerId !in suspects) {
                val claim = claims[playerId] ?: continue
                // If their role claim matches pool and no conflicts found, tentatively clear
                val isRoleInPool = claim.claimedRole.isBuffer ||
                    claims.values.count { it.claimedRole == claim.claimedRole } == 1
                if (isRoleInPool && claim.claimedEggDelta >= 0 && claim.claimedEggDelta <= 3) {
                    cleared.add(playerId)
                }
            }
        }
    }

    /**
     * Check 4: Target claim consistency.
     * - Turkey shouldn't claim visiting someone
     * - Black Swan should claim visiting self
     * - Players claiming auto-target roles (Mosquito/Tit) shouldn't name specific targets
     */
    private fun checkTargetConsistency(
        claims: Map<Int, ClaimData>,
        suspects: MutableSet<Int>,
        reasons: MutableList<String>
    ) {
        for ((playerId, claim) in claims) {
            when (claim.claimedRole) {
                RoleId.TURKEY -> {
                    if (claim.claimedTargetId != null) {
                        suspects.add(playerId)
                        reasons.add("Player $playerId claims Turkey but also claims visiting someone")
                    }
                }
                RoleId.BLACK_SWAN -> {
                    if (claim.claimedTargetId != null && claim.claimedTargetId != playerId) {
                        suspects.add(playerId)
                        reasons.add("Player $playerId claims Black Swan but claims visiting someone other than self")
                    }
                }
                else -> { /* Most roles visit others — fine */ }
            }
        }
    }
}

/**
 * Structured claim data extracted from a player's day claim.
 * Used by the solver for analysis.
 */
data class ClaimData(
    val playerId: Int,
    val claimedRole: RoleId,
    val claimedTargetId: Int?,
    val claimedEggDelta: Int
)
