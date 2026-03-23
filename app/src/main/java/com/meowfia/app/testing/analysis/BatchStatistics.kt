package com.meowfia.app.testing.analysis

import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimGameResult

data class ScorePercentiles(
    val p10: Double,
    val p25: Double,
    val p75: Double,
    val p90: Double
)

data class BatchStatistics(
    val nGames: Int,
    val nPlayers: Int,
    val nRounds: Int,

    // Score distribution
    val avgScore: Double,
    val medianScore: Double,
    val minScore: Int,
    val maxScore: Int,
    val scoreStdDev: Double,
    val negativeScorePct: Double,
    val scorePercentiles: ScorePercentiles,

    // Balance metrics
    val avgGini: Double,
    val skillPremium: Double,
    val aggroConsGap: Double,
    val catchUpRate: Double,
    val deductionCorrelation: Double,
    val strategyViability: Double,
    val strategySpread: Double,

    // Strategy breakdown
    val strategyMeans: Map<String, Double>,
    val strategyStdDevs: Map<String, Double>,

    // Night resolution
    val roleMetrics: Map<RoleId, RoleMetrics.RoleStats>,
    val nightMetrics: NightMetrics.NightStats,

    // Alignment
    val farmWinRate: Double,
    val zeroMeowfiaRate: Double,
    val allMeowfiaRate: Double,

    // Elimination
    val eliminationAccuracy: Double,

    // Per-round progression
    val perRoundAvgScores: List<Double>,

    // Role win rates
    val roleWinRates: Map<RoleId, Double>,

    // Voting patterns
    val avgVotesPerElimination: Double,
    val unanimousVoteRate: Double,

    // Comeback
    val comebackFrequency: Double,

    // Deducibility
    val solvedRate: Double,
    val narrowedRate: Double,
    val coinFlipRate: Double,
    val avgSuspectsWhenNarrowed: Double,

    // Samples
    val sampleLogs: List<SimGameResult>
)
