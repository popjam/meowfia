package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId

data class SimGameResult(
    val seed: Long,
    val config: SimConfig,
    val finalScores: List<Int>,
    val strategies: List<String>,
    val playerNames: List<String> = emptyList(),
    val roundLogs: List<SimRoundLog>,
    val perRoundDeltas: List<List<Int>>,
    val roleAssignmentCounts: Map<RoleId, Int>,
    val roleEggTotals: Map<RoleId, MutableList<Int>>,
    val alignmentWins: Map<Alignment, Int>,
    val zeroMeowfiaRounds: Int,
    val allMeowfiaRounds: Int,
    val fullLog: String
) {
    /** Fraction of eliminations that correctly targeted Meowfia. */
    val eliminationAccuracy: Double get() {
        val eliminations = roundLogs.mapNotNull { it.votingResult }
        if (eliminations.isEmpty()) return 0.0
        val correct = eliminations.count { it.eliminatedAlignment == Alignment.MEOWFIA }
        return correct.toDouble() / eliminations.size
    }

    /** Breakdown of round solvability across all rounds. */
    val solvabilityStats: Map<RoundSolver.Solvability, Int> get() {
        return roundLogs.mapNotNull { it.solvability?.solvability }
            .groupingBy { it }
            .eachCount()
    }

    /** Fraction of rounds where Meowfia could be identified from public info. */
    val solvedRate: Double get() {
        val results = roundLogs.mapNotNull { it.solvability }
        if (results.isEmpty()) return 0.0
        return results.count { it.solvability == RoundSolver.Solvability.SOLVED }.toDouble() / results.size
    }

    /** Fraction of rounds where at least one Meowfia can be identified. */
    val actionableRate: Double get() {
        val results = roundLogs.mapNotNull { it.solvability }
        if (results.isEmpty()) return 0.0
        return results.count { it.solvability == RoundSolver.Solvability.ACTIONABLE }.toDouble() / results.size
    }

    /** Fraction of rounds where suspects could be narrowed but not fully solved. */
    val narrowedRate: Double get() {
        val results = roundLogs.mapNotNull { it.solvability }
        if (results.isEmpty()) return 0.0
        return results.count { it.solvability == RoundSolver.Solvability.NARROWED }.toDouble() / results.size
    }

    /** Fraction of rounds that could be anyone. */
    val coinFlipRate: Double get() {
        val results = roundLogs.mapNotNull { it.solvability }
        if (results.isEmpty()) return 0.0
        return results.count { it.solvability == RoundSolver.Solvability.COIN_FLIP }.toDouble() / results.size
    }
}
