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
    val narrativeLog: List<String>
)

data class PoolSummary(
    val roles: List<String>,
    val flowers: List<String>
)

data class PlayerAssignmentSummary(
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
    val playerName: String,
    val roleName: String,
    val alignment: Alignment,
    val action: String,
    val targetName: String?,
    val outcome: String,
    val priority: Int
)

data class RoleChangeEntry(
    val playerName: String,
    val fromRole: String,
    val toRole: String,
    val cause: String
)

data class AlignmentChangeEntry(
    val playerName: String,
    val fromAlignment: Alignment,
    val toAlignment: Alignment,
    val cause: String
)

data class EggSummaryEntry(
    val playerName: String,
    val delta: Int,
    val breakdown: String
)

data class StatusEffectEntry(
    val playerName: String,
    val effect: StatusEffect,
    val cause: String
)

data class VisitEntry(
    val visitorName: String,
    val targetName: String?
)

data class EliminationSummary(
    val playerName: String,
    val alignment: Alignment,
    val roleName: String,
    val wasCorrectElimination: Boolean
)
