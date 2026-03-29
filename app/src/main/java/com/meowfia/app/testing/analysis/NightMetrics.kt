package com.meowfia.app.testing.analysis

import com.meowfia.app.testing.sim.SimGameResult

/** Metrics about the night resolution system. */
object NightMetrics {

    data class NightStats(
        val avgEggsCreatedPerRound: Double,
        val avgEggsStolenPerRound: Double,
        val avgNetEggsPerRound: Double,
        val avgVisitsPerPlayer: Double,
        val avgVisitorsPerPlayer: Double,
        val hugBlocks: Int,
        val avgInfoLinesPerPlayer: Double,
        val zeroEggDeltaRate: Double,
        val totalConfusedPlayers: Int,
        val avgConfusedPerRound: Double
    )

    fun analyze(results: List<SimGameResult>): NightStats {
        var totalRounds = 0
        var totalPositiveEggs = 0
        var totalNegativeEggs = 0
        var totalNetEggs = 0
        var totalZeroDeltas = 0
        var totalPlayers = 0
        var totalInfoLines = 0
        var totalConfused = 0
        var totalVisitEdges = 0
        var totalVisited = 0

        for (result in results) {
            for (log in result.roundLogs) {
                totalRounds++
                totalConfused += log.confusedPlayers

                // Visit graph stats
                totalVisitEdges += log.visitGraph.size
                val visitedSet = log.visitGraph.values.toSet()
                totalVisited += visitedSet.size

                for (report in log.dawnReports) {
                    totalPlayers++
                    val delta = report.actualEggDelta
                    if (delta > 0) totalPositiveEggs += delta
                    if (delta < 0) totalNegativeEggs += -delta
                    totalNetEggs += delta
                    if (delta == 0) totalZeroDeltas++
                    totalInfoLines += report.additionalInfo.size
                }
            }
        }

        val safeRounds = totalRounds.coerceAtLeast(1)
        val safePlayers = totalPlayers.coerceAtLeast(1)
        val nPlayers = if (results.isNotEmpty()) results.first().config.playerCount else 1

        return NightStats(
            avgEggsCreatedPerRound = totalPositiveEggs.toDouble() / safeRounds,
            avgEggsStolenPerRound = totalNegativeEggs.toDouble() / safeRounds,
            avgNetEggsPerRound = totalNetEggs.toDouble() / safeRounds,
            avgVisitsPerPlayer = totalVisitEdges.toDouble() / (safeRounds * nPlayers),
            avgVisitorsPerPlayer = totalVisited.toDouble() / (safeRounds * nPlayers),
            hugBlocks = 0, // requires role-specific tracking in handlers
            avgInfoLinesPerPlayer = totalInfoLines.toDouble() / safePlayers,
            zeroEggDeltaRate = totalZeroDeltas.toDouble() / safePlayers,
            totalConfusedPlayers = totalConfused,
            avgConfusedPerRound = totalConfused.toDouble() / safeRounds
        )
    }
}
