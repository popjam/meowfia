package com.meowfia.app.data.model

/**
 * Complete post-round analysis showing a detailed walkthrough of everything
 * that happened during the round: assignments, night actions, resolution order,
 * effects, role/alignment changes, and the final outcome.
 */
data class PostRoundAnalysis(
    val roundNumber: Int,
    val poolSummary: PoolSummary,
    val playerAssignments: List<PlayerAssignmentSummary>,
    val activeFlowers: List<FlowerSummary>,
    val resolutionOrder: List<String>,
    val nightWalkthrough: List<NightActionEntry>,
    val roleChanges: List<RoleChangeEntry>,
    val alignmentChanges: List<AlignmentChangeEntry>,
    val eggSummary: List<EggSummaryEntry>,
    val statusEffects: List<StatusEffectEntry>,
    val visitMap: List<VisitEntry>,
    val eliminationSummary: EliminationSummary?,
    val winningTeam: Alignment?,
    val narrativeLog: List<String>,
    val solvability: SolvabilityAnalysis? = null
)

data class SolvabilityAnalysis(
    val verdict: String,
    val verdictExplanation: String,
    val suspects: List<String>,
    val cleared: List<String>,
    val consistentWorlds: Int,
    val totalCandidates: Int,
    val reasons: List<String>,
    /** What each player claimed during the day. */
    val playerClaims: List<PlayerClaimSummary>,
    /** Per-player suspicion: name → 0–100% how often they appear as Meowfia in consistent worlds. */
    val suspicionRanking: List<SuspicionEntry> = emptyList(),
    /** Each possible world described as a list of who would be Meowfia. */
    val worldDescriptions: List<WorldDescription>
) {
    /** 0–100 percentage of how solvable the round was.
     *  100% = fully solved (exactly 1 world), 0% = no info (coin flip).
     *  0 consistent worlds = contradictory claims, not truly solved. */
    val solvabilityPercent: Int get() {
        if (totalCandidates <= 1) return 100
        if (consistentWorlds == 0) {
            // All worlds eliminated = contradictory claims.
            // If suspects were narrowed, show high %. Otherwise 50%.
            val suspectRatio = if (suspects.isNotEmpty()) {
                (1.0 - suspects.size.toDouble() / (suspects.size + cleared.size).coerceAtLeast(1)) * 100
            } else 50.0
            return suspectRatio.toInt().coerceIn(30, 95)
        }
        if (consistentWorlds == 1) return 100
        return ((1.0 - consistentWorlds.toDouble() / totalCandidates) * 100).toInt().coerceIn(0, 100)
    }
}

data class SuspicionEntry(
    val playerName: String,
    val percent: Int,  // 0–100
    val isActualMeowfia: Boolean
)

data class PlayerClaimSummary(
    val playerId: Int,
    val playerName: String,
    val claimedRole: String,
    val claimedTarget: String?,
    val claimedEggDelta: Int,
    val actualRole: String,
    val actualAlignment: String,
    val wasLying: Boolean
)

data class WorldDescription(
    val meowfiaNames: List<String>,
    val farmNames: List<String>,
    val isActualWorld: Boolean,
    /** Per-player assumed role in this world (name → role display name). */
    val assumedRoles: Map<String, String> = emptyMap()
)

data class PoolSummary(
    val roles: List<String>,
    val flowers: List<String>
)

data class PlayerAssignmentSummary(
    val playerId: Int,
    val playerName: String,
    val alignment: Alignment,
    val roleName: String,
    val roleDescription: String,
    val isBot: Boolean
)

data class FlowerSummary(
    val name: String,
    val description: String
)

data class NightActionEntry(
    val playerId: Int,
    val playerName: String,
    val roleName: String,
    val alignment: Alignment,
    val action: String,
    val targetName: String?,
    val outcome: String,
    val priority: Int
)

data class RoleChangeEntry(
    val playerId: Int,
    val playerName: String,
    val fromRole: String,
    val toRole: String,
    val cause: String
)

data class AlignmentChangeEntry(
    val playerId: Int,
    val playerName: String,
    val fromAlignment: Alignment,
    val toAlignment: Alignment,
    val cause: String
)

data class EggSummaryEntry(
    val playerId: Int,
    val playerName: String,
    val delta: Int,
    val breakdown: String
)

data class StatusEffectEntry(
    val playerId: Int,
    val playerName: String,
    val effect: StatusEffect,
    val cause: String
)

data class VisitEntry(
    val visitorId: Int,
    val visitorName: String,
    val targetName: String?
)

data class EliminationSummary(
    val playerId: Int,
    val playerName: String,
    val alignment: Alignment,
    val roleName: String,
    val wasCorrectElimination: Boolean
)
