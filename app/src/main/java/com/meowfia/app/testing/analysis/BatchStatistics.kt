package com.meowfia.app.testing.analysis

import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimGameResult

data class WinLossPatternStats(
    val pattern: String,       // e.g. "W-W-L", "L-W-W"
    val occurrences: Int,
    val avgFinalScore: Double,
    val winnerCount: Int       // how many players with this pattern won the game
)

data class LuckBucket(
    val label: String,       // e.g. "Low (1-4)", "Mid (5-8)", "High (9-13)"
    val avgHandValue: Double,
    val avgFinalScore: Double,
    val count: Int
)

data class RecordFacts(
    val mostPointsSingleRound: RecordEntry? = null,
    val mostEggsGained: RecordEntry? = null,
    val mostEggsLost: RecordEntry? = null,
    val mostCardsBet: RecordEntry? = null,
    val biggestLoss: RecordEntry? = null,
    val biggestCatchUp: RecordEntry? = null,
    val highestFinalScore: RecordEntry? = null,
    val lowestFinalScore: RecordEntry? = null,
    val bestRole: String? = null,
    val worstRole: String? = null,
    val bestWinLossPattern: String? = null,
    val worstWinLossPattern: String? = null,
    val bestAlignmentPattern: String? = null,
    val worstAlignmentPattern: String? = null,
    val bestArchetype: String? = null,
    val drawCount: Int = 0,
    val highestCardsBetAgainstOne: RecordEntry? = null,
    val roundsWithNoBuffers: Int = 0,
    val mostRoleSwaps: RecordEntry? = null,
    val mostConfused: RecordEntry? = null,
    val mostHugged: RecordEntry? = null,
    val mostWinks: RecordEntry? = null
)

data class RecordEntry(
    val playerName: String,
    val value: Int,
    val roundNum: Int = 0,
    val gameIndex: Int = 0
)

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
    val actionableRate: Double = 0.0,
    val narrowedRate: Double,
    val coinFlipRate: Double,
    val avgSuspectsWhenNarrowed: Double,

    // Solvability distribution
    val solvabilityPercentages: List<Int> = emptyList(),
    val avgSolvabilityPercent: Double = 0.0,
    val meowfiaCountDistribution: Map<Int, Int> = emptyMap(),
    val meowfiaCountWinRates: Map<Int, Double> = emptyMap(),

    // Win/Loss patterns
    val winLossPatterns: Map<String, WinLossPatternStats> = emptyMap(),
    val alignmentPatterns: Map<String, WinLossPatternStats> = emptyMap(),

    // Hand luck correlation
    val handLuckCorrelation: Double = 0.0,
    val handLuckBuckets: List<LuckBucket> = emptyList(),

    // Record facts
    val recordFacts: RecordFacts = RecordFacts(),

    // Samples
    val sampleLogs: List<SimGameResult>
)
