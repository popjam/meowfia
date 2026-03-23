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
        val nestCapHits: Int,
        val hugBlocks: Int,
        val avgInfoLinesPerPlayer: Double,
        val emptyNestRate: Double
    )

    fun analyze(results: List<SimGameResult>): NightStats {
        var totalRounds = 0
        var totalEggs = 0
        var totalEmptyNests = 0
        var totalPlayers = 0
        var totalInfoLines = 0

        for (result in results) {
            for (log in result.roundLogs) {
                totalRounds++
                for (report in log.dawnReports) {
                    totalPlayers++
                    totalEggs += report.actualNestEggs
                    if (report.actualNestEggs == 0) totalEmptyNests++
                    totalInfoLines += report.additionalInfo.size
                }
            }
        }

        val safeRounds = totalRounds.coerceAtLeast(1)
        val safePlayers = totalPlayers.coerceAtLeast(1)

        return NightStats(
            avgEggsCreatedPerRound = totalEggs.toDouble() / safeRounds,
            avgEggsStolenPerRound = 0.0, // TODO: track steal events
            avgNetEggsPerRound = totalEggs.toDouble() / safeRounds,
            avgVisitsPerPlayer = 0.0, // TODO: track from visit graph
            avgVisitorsPerPlayer = 0.0,
            nestCapHits = 0,
            hugBlocks = 0,
            avgInfoLinesPerPlayer = totalInfoLines.toDouble() / safePlayers,
            emptyNestRate = totalEmptyNests.toDouble() / safePlayers
        )
    }
}
