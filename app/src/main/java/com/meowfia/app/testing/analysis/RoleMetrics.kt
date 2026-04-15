package com.meowfia.app.testing.analysis

import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimGameResult

/** Per-role analysis — assignment frequency, egg economy, win rates. */
object RoleMetrics {

    data class RoleStats(
        val roleId: RoleId,
        val timesAssigned: Int,
        val avgEggDelta: Double,
        val totalEggsProduced: Int,
        val totalEggsLost: Int,
        val teamWinRate: Double
    )

    fun analyze(results: List<SimGameResult>): Map<RoleId, RoleStats> {
        data class Accumulator(
            var timesAssigned: Int = 0,
            var totalEggDelta: Int = 0,
            var totalPositiveEggs: Int = 0,
            var totalNegativeEggs: Int = 0,
            var teamWins: Int = 0
        )

        val accumulators = mutableMapOf<RoleId, Accumulator>()

        for (result in results) {
            for (log in result.roundLogs) {
                val winningTeam = log.votingResult?.winningTeam
                for (a in log.assignments) {
                    val acc = accumulators.getOrPut(a.roleId) { Accumulator() }
                    acc.timesAssigned++
                    val dawn = log.dawnReports.find { it.playerId == a.playerId }
                    if (dawn != null) {
                        acc.totalEggDelta += dawn.actualEggDelta
                        if (dawn.actualEggDelta > 0) acc.totalPositiveEggs += dawn.actualEggDelta
                        if (dawn.actualEggDelta < 0) acc.totalNegativeEggs += -dawn.actualEggDelta
                    }
                    if (winningTeam != null && a.alignment == winningTeam) {
                        acc.teamWins++
                    }
                }
            }
        }

        return accumulators.map { (roleId, acc) ->
            val safe = acc.timesAssigned.coerceAtLeast(1)
            roleId to RoleStats(
                roleId = roleId,
                timesAssigned = acc.timesAssigned,
                avgEggDelta = acc.totalEggDelta.toDouble() / safe,
                totalEggsProduced = acc.totalPositiveEggs,
                totalEggsLost = acc.totalNegativeEggs,
                teamWinRate = acc.teamWins.toDouble() / safe
            )
        }.toMap()
    }
}
