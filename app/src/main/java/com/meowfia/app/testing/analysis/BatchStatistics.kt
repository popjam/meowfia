package com.meowfia.app.testing.analysis

import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimGameResult
import com.meowfia.app.testing.sim.Suit

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

    // Suit economics
    val suitThrows: Map<Suit, Int>,
    val suitWins: Map<Suit, Int>,
    val suitLosses: Map<Suit, Int>,
    val clubGiftsTotal: Int,
    val clubGiftsAvgValue: Double,
    val spadeSteals: Int,
    val spadeGives: Int,
    val diamondLocks: Int,
    val diamondDemotes: Int,

    // Night resolution
    val roleMetrics: Map<RoleId, RoleMetrics.RoleStats>,
    val nightMetrics: NightMetrics.NightStats,

    // Alignment
    val farmWinRate: Double,
    val zeroMeowfiaRate: Double,
    val allMeowfiaRate: Double,

    // Samples
    val sampleLogs: List<SimGameResult>
)
