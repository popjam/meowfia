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
        val zeroEggDeltaRate: Double
    )

    fun analyze(results: List<SimGameResult>): NightStats {
        var totalRounds = 0
        var totalPositiveEggs = 0
        var totalZeroDeltas = 0
        var totalPlayers = 0
        var totalInfoLines = 0

        for (result in results) {
            for (log in result.roundLogs) {
                totalRounds++
                for (report in log.dawnReports) {
                    totalPlayers++
                    if (report.actualEggDelta > 0) totalPositiveEggs += report.actualEggDelta
                    if (report.actualEggDelta == 0) totalZeroDeltas++
                    totalInfoLines += report.additionalInfo.size
                }
            }
        }

        val safeRounds = totalRounds.coerceAtLeast(1)
        val safePlayers = totalPlayers.coerceAtLeast(1)

        return NightStats(
            avgEggsCreatedPerRound = totalPositiveEggs.toDouble() / safeRounds,
            avgEggsStolenPerRound = 0.0, // TODO: track steal events
            avgNetEggsPerRound = totalPositiveEggs.toDouble() / safeRounds,
            avgVisitsPerPlayer = 0.0, // TODO: track from visit graph
            avgVisitorsPerPlayer = 0.0,
            hugBlocks = 0,
            avgInfoLinesPerPlayer = totalInfoLines.toDouble() / safePlayers,
            zeroEggDeltaRate = totalZeroDeltas.toDouble() / safePlayers
        )
    }
}
