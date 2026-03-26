package com.meowfia.app.testing.analysis

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.RoundSolver
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

        // Solvability
        val allSolvability = results.flatMap { game ->
            game.roundLogs.mapNotNull { it.solvability }
        }
        val solvabilityTotal = allSolvability.size.coerceAtLeast(1)
        val solvedRate = allSolvability.count { it.solvability == RoundSolver.Solvability.SOLVED }.toDouble() / solvabilityTotal
        val actionableRate = allSolvability.count { it.solvability == RoundSolver.Solvability.ACTIONABLE }.toDouble() / solvabilityTotal
        val narrowedRate = allSolvability.count { it.solvability == RoundSolver.Solvability.NARROWED }.toDouble() / solvabilityTotal
        val coinFlipRate = allSolvability.count { it.solvability == RoundSolver.Solvability.COIN_FLIP }.toDouble() / solvabilityTotal
        val narrowedResults = allSolvability.filter { it.solvability == RoundSolver.Solvability.NARROWED }
        val avgSuspectsWhenNarrowed = if (narrowedResults.isNotEmpty()) {
            narrowedResults.map { it.suspects.size.toDouble() }.average()
        } else 0.0
        val narrowedBySuspectCount = if (narrowedResults.isNotEmpty()) {
            narrowedResults.groupingBy { it.suspects.size }.eachCount()
                .mapValues { (_, count) -> count.toDouble() / solvabilityTotal }
        } else emptyMap()

        // Solvability percentages from all rounds
        val solvabilityPercentages = allSolvability
            .filter { it.totalCandidates > 0 }
            .map { ((1.0 - it.consistentWorlds.toDouble() / it.totalCandidates) * 100).toInt().coerceIn(0, 100) }
        val avgSolvabilityPercent = if (solvabilityPercentages.isNotEmpty()) {
            solvabilityPercentages.average()
        } else 0.0

        // Meowfia count distribution and win rates
        val meowfiaCountBuckets = mutableMapOf<Int, Int>()
        val meowfiaCountFarmWins = mutableMapOf<Int, Int>()
        val meowfiaCountTotal = mutableMapOf<Int, Int>()
        for (result in results) {
            for (log in result.roundLogs) {
                val mc = log.meowfiaCount
                meowfiaCountBuckets[mc] = (meowfiaCountBuckets[mc] ?: 0) + 1
                meowfiaCountTotal[mc] = (meowfiaCountTotal[mc] ?: 0) + 1
                val vr = log.votingResult
                if (vr != null && vr.winningTeam == Alignment.FARM) {
                    meowfiaCountFarmWins[mc] = (meowfiaCountFarmWins[mc] ?: 0) + 1
                }
            }
        }
        val meowfiaCountWinRates = meowfiaCountTotal.mapValues { (mc, total) ->
            (meowfiaCountFarmWins[mc] ?: 0).toDouble() / total
        }

        // Win/Loss patterns
        data class PatternAccum(var count: Int = 0, var totalScore: Double = 0.0, var winners: Int = 0)
        val patternMap = mutableMapOf<String, PatternAccum>()
        for (result in results) {
            val nPlayers = result.config.playerCount
            val gameWinner = result.finalScores.indices.maxByOrNull { result.finalScores[it] } ?: -1
            for (playerIdx in 0 until nPlayers) {
                val pattern = result.roundLogs.mapNotNull { log ->
                    val vr = log.votingResult ?: return@mapNotNull null
                    val assignment = log.assignments.find { it.playerId == playerIdx } ?: return@mapNotNull null
                    if (assignment.alignment == vr.winningTeam) "W" else "L"
                }.joinToString("-")
                if (pattern.isEmpty()) continue
                val acc = patternMap.getOrPut(pattern) { PatternAccum() }
                acc.count++
                acc.totalScore += result.finalScores[playerIdx]
                if (playerIdx == gameWinner) acc.winners++
            }
        }
        val winLossPatterns = patternMap.mapValues { (key, acc) ->
            WinLossPatternStats(
                pattern = key,
                occurrences = acc.count,
                avgFinalScore = if (acc.count > 0) acc.totalScore / acc.count else 0.0,
                winnerCount = acc.winners
            )
        }

        // Alignment patterns (F-F-M, M-F-F etc.)
        val alignPatternMap = mutableMapOf<String, PatternAccum>()
        for (result in results) {
            val nPlayers = result.config.playerCount
            val gameWinner = result.finalScores.indices.maxByOrNull { result.finalScores[it] } ?: -1
            for (playerIdx in 0 until nPlayers) {
                val pattern = result.roundLogs.map { log ->
                    val assignment = log.assignments.find { it.playerId == playerIdx }
                    if (assignment?.alignment == com.meowfia.app.data.model.Alignment.MEOWFIA) "M" else "F"
                }.joinToString("-")
                if (pattern.isEmpty()) continue
                val acc = alignPatternMap.getOrPut(pattern) { PatternAccum() }
                acc.count++
                acc.totalScore += result.finalScores[playerIdx]
                if (playerIdx == gameWinner) acc.winners++
            }
        }
        val alignmentPatterns = alignPatternMap.mapValues { (key, acc) ->
            WinLossPatternStats(pattern = key, occurrences = acc.count,
                avgFinalScore = if (acc.count > 0) acc.totalScore / acc.count else 0.0, winnerCount = acc.winners)
        }

        // Hand luck correlation: average hand card value vs final score
        val handValues = mutableListOf<Double>()
        val handScores = mutableListOf<Double>()
        for (result in results) {
            for (playerIdx in 0 until result.config.playerCount) {
                var totalCardValue = 0
                var totalCards = 0
                for (log in result.roundLogs) {
                    val vr = log.votingResult ?: continue
                    val thrown = vr.thrown[playerIdx] ?: emptyList()
                    val kept = vr.kept[playerIdx] ?: emptyList()
                    totalCardValue += thrown.sumOf { it.value } + kept.sumOf { it.value }
                    totalCards += thrown.size + kept.size
                }
                if (totalCards > 0) {
                    handValues.add(totalCardValue.toDouble() / totalCards)
                    handScores.add(result.finalScores[playerIdx].toDouble())
                }
            }
        }
        val handLuckCorrelation = pearson(handValues, handScores)

        // Luck buckets (low/mid/high avg card value → avg score)
        data class LuckAccum(var count: Int = 0, var totalHandVal: Double = 0.0, var totalScore: Double = 0.0)
        val bucketMap = mutableMapOf("Low (1-4)" to LuckAccum(), "Mid (5-8)" to LuckAccum(), "High (9-13)" to LuckAccum())
        for (i in handValues.indices) {
            val bucket = when {
                handValues[i] < 4.5 -> "Low (1-4)"
                handValues[i] < 8.5 -> "Mid (5-8)"
                else -> "High (9-13)"
            }
            val acc = bucketMap[bucket]!!
            acc.count++
            acc.totalHandVal += handValues[i]
            acc.totalScore += handScores[i]
        }
        val handLuckBuckets = listOf("Low (1-4)", "Mid (5-8)", "High (9-13)").map { label ->
            val acc = bucketMap[label]!!
            LuckBucket(label, if (acc.count > 0) acc.totalHandVal / acc.count else 0.0,
                if (acc.count > 0) acc.totalScore / acc.count else 0.0, acc.count)
        }

        // Record facts
        var bestPointsGain: RecordEntry? = null
        var bestEggsGained: RecordEntry? = null
        var worstEggsLost: RecordEntry? = null
        var mostCardsBet: RecordEntry? = null
        var biggestLoss: RecordEntry? = null
        var biggestCatchUp: RecordEntry? = null

        for ((gameIdx, result) in results.withIndex()) {
            val nPlayers = result.config.playerCount
            for (log in result.roundLogs) {
                val vr = log.votingResult ?: continue
                for (i in 0 until nPlayers) {
                    val name = result.playerNames.getOrElse(i) { "P$i" }
                    val pre = log.preScores.getOrElse(i) { 0 }
                    val post = log.postScores.getOrElse(i) { 0 }
                    val delta = post - pre
                    val dawn = log.dawnReports.find { it.playerId == i }
                    val eggDelta = dawn?.actualEggDelta ?: 0
                    val thrown = vr.thrown[i]?.size ?: 0

                    if (delta > (bestPointsGain?.value ?: 0))
                        bestPointsGain = RecordEntry(name, delta, log.roundNum, gameIdx)
                    if (eggDelta > (bestEggsGained?.value ?: 0))
                        bestEggsGained = RecordEntry(name, eggDelta, log.roundNum, gameIdx)
                    if (eggDelta < (worstEggsLost?.value ?: 0))
                        worstEggsLost = RecordEntry(name, eggDelta, log.roundNum, gameIdx)
                    if (thrown > (mostCardsBet?.value ?: 0))
                        mostCardsBet = RecordEntry(name, thrown, log.roundNum, gameIdx)
                    if (delta < (biggestLoss?.value ?: 0))
                        biggestLoss = RecordEntry(name, delta, log.roundNum, gameIdx)
                }
            }
            // Biggest catch-up: biggest rank improvement from mid to end
            val nRounds = result.roundLogs.size
            if (nRounds >= 3) {
                val midIdx = nRounds / 2 - 1
                for (i in 0 until nPlayers) {
                    val midScore = result.perRoundDeltas.getOrNull(i)?.getOrNull(midIdx) ?: 0
                    val finalScore = result.finalScores[i]
                    val catchUp = finalScore - midScore
                    if (catchUp > (biggestCatchUp?.value ?: 0)) {
                        val name = result.playerNames.getOrElse(i) { "P$i" }
                        biggestCatchUp = RecordEntry(name, catchUp, 0, gameIdx)
                    }
                }
            }
        }
        // Additional records
        var highestFinalScore: RecordEntry? = null
        var lowestFinalScore: RecordEntry? = null
        var highestCardsBetAgainstOne: RecordEntry? = null
        var drawCount = 0
        var roundsWithNoBuffers = 0

        for ((gameIdx, result) in results.withIndex()) {
            for (i in result.finalScores.indices) {
                val name = result.playerNames.getOrElse(i) { "P$i" }
                val score = result.finalScores[i]
                if (score > (highestFinalScore?.value ?: Int.MIN_VALUE))
                    highestFinalScore = RecordEntry(name, score, 0, gameIdx)
                if (score < (lowestFinalScore?.value ?: Int.MAX_VALUE))
                    lowestFinalScore = RecordEntry(name, score, 0, gameIdx)
            }
            for (log in result.roundLogs) {
                // Draws (ties in votes)
                val vr = log.votingResult ?: continue
                val maxVotes = vr.votes.values.maxOrNull() ?: 0
                val tiedCount = vr.votes.values.count { it == maxVotes }
                if (tiedCount > 1) drawCount++

                // Most cards bet against a single player
                val votesPerTarget = mutableMapOf<Int, Int>()
                for ((pid, cards) in vr.thrown) {
                    val target = vr.targets[pid] ?: continue
                    votesPerTarget[target] = (votesPerTarget[target] ?: 0) + cards.size
                }
                for ((targetId, totalCards) in votesPerTarget) {
                    if (totalCards > (highestCardsBetAgainstOne?.value ?: 0)) {
                        val targetName = result.playerNames.getOrElse(targetId) { "P$targetId" }
                        highestCardsBetAgainstOne = RecordEntry(targetName, totalCards, log.roundNum, gameIdx)
                    }
                }

                // Rounds with no buffer roles (no Pigeon/House Cat in assignments)
                val hasBuffer = log.assignments.any {
                    it.roleId == com.meowfia.app.data.model.RoleId.PIGEON ||
                    it.roleId == com.meowfia.app.data.model.RoleId.HOUSE_CAT
                }
                if (!hasBuffer) roundsWithNoBuffers++
            }
        }

        // Status effect records across all games
        var mostRoleSwaps: RecordEntry? = null
        var mostConfused: RecordEntry? = null
        var mostHugged: RecordEntry? = null
        var mostWinks: RecordEntry? = null
        for ((gameIdx, result) in results.withIndex()) {
            for (log in result.roundLogs) {
                if (log.roleSwapCount > (mostRoleSwaps?.value ?: 0))
                    mostRoleSwaps = RecordEntry("", log.roleSwapCount, log.roundNum, gameIdx)
                if (log.confusedPlayers > (mostConfused?.value ?: 0))
                    mostConfused = RecordEntry("", log.confusedPlayers, log.roundNum, gameIdx)
                if (log.huggedPlayers > (mostHugged?.value ?: 0))
                    mostHugged = RecordEntry("", log.huggedPlayers, log.roundNum, gameIdx)
                if (log.winkPlayers > (mostWinks?.value ?: 0))
                    mostWinks = RecordEntry("", log.winkPlayers, log.roundNum, gameIdx)
            }
        }

        // Best/worst from computed maps
        val bestRole = roleMetrics.maxByOrNull { it.value.teamWinRate }?.key?.displayName
        val worstRole = roleMetrics.filter { it.value.timesAssigned >= 5 }.minByOrNull { it.value.teamWinRate }?.key?.displayName
        val bestWL = winLossPatterns.values.filter { it.occurrences >= 3 }.maxByOrNull { it.winnerCount.toDouble() / it.occurrences }?.pattern
        val worstWL = winLossPatterns.values.filter { it.occurrences >= 3 }.minByOrNull { it.winnerCount.toDouble() / it.occurrences }?.pattern
        val bestAP = alignmentPatterns.values.filter { it.occurrences >= 3 }.maxByOrNull { it.winnerCount.toDouble() / it.occurrences }?.pattern
        val worstAP = alignmentPatterns.values.filter { it.occurrences >= 3 }.minByOrNull { it.winnerCount.toDouble() / it.occurrences }?.pattern
        val bestArchetype = strategyMeans.maxByOrNull { it.value }?.key

        val recordFacts = RecordFacts(
            mostPointsSingleRound = bestPointsGain,
            mostEggsGained = bestEggsGained,
            mostEggsLost = worstEggsLost,
            mostCardsBet = mostCardsBet,
            biggestLoss = biggestLoss,
            biggestCatchUp = biggestCatchUp,
            highestFinalScore = highestFinalScore,
            lowestFinalScore = lowestFinalScore,
            bestRole = bestRole,
            worstRole = worstRole,
            bestWinLossPattern = bestWL,
            worstWinLossPattern = worstWL,
            bestAlignmentPattern = bestAP,
            worstAlignmentPattern = worstAP,
            bestArchetype = bestArchetype,
            drawCount = drawCount,
            highestCardsBetAgainstOne = highestCardsBetAgainstOne,
            roundsWithNoBuffers = roundsWithNoBuffers,
            mostRoleSwaps = mostRoleSwaps,
            mostConfused = mostConfused,
            mostHugged = mostHugged,
            mostWinks = mostWinks
        )

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
            solvedRate = solvedRate,
            actionableRate = actionableRate,
            narrowedRate = narrowedRate,
            coinFlipRate = coinFlipRate,
            avgSuspectsWhenNarrowed = avgSuspectsWhenNarrowed,
            narrowedBySuspectCount = narrowedBySuspectCount,
            solvabilityPercentages = solvabilityPercentages,
            avgSolvabilityPercent = avgSolvabilityPercent,
            meowfiaCountDistribution = meowfiaCountBuckets,
            meowfiaCountWinRates = meowfiaCountWinRates,
            winLossPatterns = winLossPatterns,
            alignmentPatterns = alignmentPatterns,
            handLuckCorrelation = handLuckCorrelation,
            handLuckBuckets = handLuckBuckets,
            recordFacts = recordFacts,
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
