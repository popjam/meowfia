package com.meowfia.app.testing.analysis

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimGameResult
import kotlin.math.abs
import kotlin.math.sqrt

/** Computes all aggregate metrics from a batch of game results. */
object BalanceMetrics {

    fun analyze(
        results: List<SimGameResult>,
        config: SimConfig,
        sampleLogs: List<SimGameResult>
    ): BatchStatistics {
        val flatScores = results.flatMap { it.finalScores }
        val allGinis = results.map { gini(it.finalScores.map { s -> s.toDouble() }) }

        // Strategy performance
        val strategyScores = mutableMapOf<String, MutableList<Int>>()
        for (result in results) {
            for (i in result.strategies.indices) {
                strategyScores.getOrPut(result.strategies[i]) { mutableListOf() }
                    .add(result.finalScores[i])
            }
        }

        val strategyMeans = strategyScores.mapValues { it.value.average() }
        val strategyStdDevs = strategyScores.mapValues { stdDev(it.value.map { v -> v.toDouble() }) }

        // Skill premium
        val skilledAvg = listOf("Aggressive-Skilled", "Conservative-Skilled", "Balanced-Skilled")
            .mapNotNull { strategyMeans[it] }.average()
        val unskilledAvg = listOf("Aggressive-Unskilled", "Conservative-Unskilled", "Balanced-Random")
            .mapNotNull { strategyMeans[it] }.average()
        val skillPremium = if (skilledAvg.isNaN() || unskilledAvg.isNaN()) 0.0 else skilledAvg - unskilledAvg

        // Aggro-conservative gap
        val aggroAvg = listOf("Aggressive-Skilled", "Aggressive-Unskilled")
            .mapNotNull { strategyMeans[it] }.average()
        val consAvg = listOf("Conservative-Skilled", "Conservative-Unskilled")
            .mapNotNull { strategyMeans[it] }.average()
        val aggroConsGap = if (aggroAvg.isNaN() || consAvg.isNaN()) 0.0 else aggroAvg - consAvg

        // Strategy viability: % of archetypes within 80% of best
        val bestMean = strategyMeans.values.maxOrNull() ?: 1.0
        val viableCount = strategyMeans.values.count { it >= bestMean * 0.8 }
        val viability = viableCount.toDouble() / strategyMeans.size.coerceAtLeast(1)

        // Catch-up rate: rank mobility from mid-game to final
        val catchUpRates = mutableListOf<Double>()
        for (result in results) {
            val nPlayers = result.config.playerCount
            val nRounds = result.roundLogs.size
            if (nRounds < 3) continue

            val midRoundIdx = nRounds / 2 - 1
            val midScores = result.perRoundDeltas.map { it.getOrElse(midRoundIdx) { 0 } }
            val finalScores = result.finalScores

            val midRanking = (0 until nPlayers).sortedByDescending { midScores[it] }
            val finalRanking = (0 until nPlayers).sortedByDescending { finalScores[it] }

            val rankDiff = (0 until nPlayers).sumOf { playerId ->
                abs(midRanking.indexOf(playerId) - finalRanking.indexOf(playerId))
            }
            val maxDiff = nPlayers * (nPlayers - 1) / 2
            catchUpRates.add(rankDiff.toDouble() / maxDiff.coerceAtLeast(1))
        }
        val catchUpRate = if (catchUpRates.isNotEmpty()) catchUpRates.average() else 0.0

        // Deduction correlation: correct-throw accuracy vs final score
        val accuracies = mutableListOf<Double>()
        val corrScores = mutableListOf<Double>()
        for (result in results) {
            val nPlayers = result.config.playerCount
            for (playerIdx in 0 until nPlayers) {
                var correctThrows = 0
                var totalThrows = 0
                for (roundLog in result.roundLogs) {
                    val vr = roundLog.votingResult ?: continue
                    val assignment = roundLog.assignments.find { it.playerId == playerIdx } ?: continue
                    totalThrows++
                    if (assignment.alignment == vr.winningTeam) {
                        correctThrows++
                    }
                }
                if (totalThrows > 0) {
                    accuracies.add(correctThrows.toDouble() / totalThrows)
                    corrScores.add(result.finalScores[playerIdx].toDouble())
                }
            }
        }
        val deductionCorrelation = pearson(accuracies, corrScores)

        // Alignment
        val totalFarmWins = results.sumOf { it.alignmentWins[Alignment.FARM] ?: 0 }
        val totalRounds = results.sumOf { it.roundLogs.size }

        // Night resolution metrics
        val roleMetrics = RoleMetrics.analyze(results)
        val nightMetrics = NightMetrics.analyze(results)

        // Score percentiles
        val sortedScores = flatScores.sorted()
        val scorePercentiles = if (sortedScores.isNotEmpty()) {
            ScorePercentiles(
                p10 = percentile(sortedScores, 0.10),
                p25 = percentile(sortedScores, 0.25),
                p75 = percentile(sortedScores, 0.75),
                p90 = percentile(sortedScores, 0.90)
            )
        } else {
            ScorePercentiles(0.0, 0.0, 0.0, 0.0)
        }

        // Elimination accuracy
        val eliminationAccuracy = if (results.isNotEmpty()) {
            results.map { it.eliminationAccuracy }.average()
        } else 0.0

        // Per-round avg scores
        val maxRounds = results.maxOfOrNull { it.roundLogs.size } ?: 0
        val perRoundAvgScores = (0 until maxRounds).map { roundIdx ->
            val scoresAtRound = results.flatMap { result ->
                result.perRoundDeltas.mapNotNull { deltas ->
                    deltas.getOrNull(roundIdx)
                }
            }
            if (scoresAtRound.isNotEmpty()) scoresAtRound.average() else 0.0
        }

        // Role win rates
        val roleWinRates = roleMetrics.mapValues { it.value.teamWinRate }

        // Voting patterns
        var totalVotesOnEliminated = 0
        var unanimousRounds = 0
        var votingRounds = 0
        for (result in results) {
            for (log in result.roundLogs) {
                val vr = log.votingResult ?: continue
                votingRounds++
                val elimVotes = vr.votes[vr.eliminatedId] ?: 0
                totalVotesOnEliminated += elimVotes
                val totalVotes = vr.votes.values.sum()
                if (elimVotes == totalVotes) unanimousRounds++
            }
        }
        val avgVotesPerElimination = if (votingRounds > 0) {
            totalVotesOnEliminated.toDouble() / votingRounds
        } else 0.0
        val unanimousVoteRate = if (votingRounds > 0) {
            unanimousRounds.toDouble() / votingRounds
        } else 0.0

        // Comeback frequency: leader at midpoint didn't win
        var comebackCount = 0
        var eligibleGames = 0
        for (result in results) {
            val nRounds = result.roundLogs.size
            if (nRounds < 3) continue
            eligibleGames++
            val midRoundIdx = nRounds / 2 - 1
            val midScores = result.perRoundDeltas.map { it.getOrElse(midRoundIdx) { 0 } }
            val midLeader = midScores.indices.maxByOrNull { midScores[it] } ?: continue
            val finalWinner = result.finalScores.indices.maxByOrNull { result.finalScores[it] } ?: continue
            if (midLeader != finalWinner) comebackCount++
        }
        val comebackFrequency = if (eligibleGames > 0) {
            comebackCount.toDouble() / eligibleGames
        } else 0.0

        return BatchStatistics(
            nGames = results.size,
            nPlayers = config.playerCount,
            nRounds = config.roundCount,
            avgScore = flatScores.average(),
            medianScore = sortedScores.let { if (it.isNotEmpty()) it[it.size / 2].toDouble() else 0.0 },
            minScore = flatScores.minOrNull() ?: 0,
            maxScore = flatScores.maxOrNull() ?: 0,
            scoreStdDev = stdDev(flatScores.map { it.toDouble() }),
            negativeScorePct = flatScores.count { it < 0 } * 100.0 / flatScores.size.coerceAtLeast(1),
            scorePercentiles = scorePercentiles,
            avgGini = allGinis.average(),
            skillPremium = skillPremium,
            aggroConsGap = aggroConsGap,
            catchUpRate = catchUpRate,
            deductionCorrelation = deductionCorrelation,
            strategyViability = viability,
            strategySpread = bestMean - (strategyMeans.values.minOrNull() ?: 0.0),
            strategyMeans = strategyMeans,
            strategyStdDevs = strategyStdDevs,
            roleMetrics = roleMetrics,
            nightMetrics = nightMetrics,
            farmWinRate = if (totalRounds > 0) totalFarmWins.toDouble() / totalRounds else 0.5,
            zeroMeowfiaRate = results.sumOf { it.zeroMeowfiaRounds }.toDouble() / totalRounds.coerceAtLeast(1),
            allMeowfiaRate = results.sumOf { it.allMeowfiaRounds }.toDouble() / totalRounds.coerceAtLeast(1),
            eliminationAccuracy = eliminationAccuracy,
            perRoundAvgScores = perRoundAvgScores,
            roleWinRates = roleWinRates,
            avgVotesPerElimination = avgVotesPerElimination,
            unanimousVoteRate = unanimousVoteRate,
            comebackFrequency = comebackFrequency,
            sampleLogs = sampleLogs
        )
    }

    private fun percentile(sorted: List<Int>, p: Double): Double {
        if (sorted.isEmpty()) return 0.0
        val idx = (p * (sorted.size - 1)).toInt()
        return sorted[idx].toDouble()
    }

    fun gini(values: List<Double>): Double {
        val sorted = values.map { maxOf(0.0, it) }.sorted()
        val n = sorted.size
        val total = sorted.sum()
        if (total == 0.0) return 0.0
        return sorted.mapIndexed { i, v ->
            (2.0 * (i + 1) - n - 1) * v
        }.sum() / (n * total)
    }

    fun pearson(xs: List<Double>, ys: List<Double>): Double {
        val n = xs.size
        if (n < 2) return 0.0
        val mx = xs.average()
        val my = ys.average()
        val cov = xs.zip(ys).sumOf { (x, y) -> (x - mx) * (y - my) }
        val vx = xs.sumOf { (it - mx) * (it - mx) }
        val vy = ys.sumOf { (it - my) * (it - my) }
        if (vx == 0.0 || vy == 0.0) return 0.0
        return cov / sqrt(vx * vy)
    }

    fun stdDev(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }
}
