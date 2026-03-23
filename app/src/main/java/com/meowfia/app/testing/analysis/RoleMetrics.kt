package com.meowfia.app.testing.analysis

import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimGameResult

/** Per-role analysis — assignment frequency, egg economy, etc. */
object RoleMetrics {

    data class RoleStats(
        val roleId: RoleId,
        val timesAssigned: Int,
        val avgEggsProduced: Double,
        val avgEggsGainedSelf: Double,
        val stealAttempts: Int,
        val stealSuccesses: Int,
        val avgVisitorsWhenPassive: Double,
        val investigationCount: Int,
        val correctInvestigations: Int
    )

    fun analyze(results: List<SimGameResult>): Map<RoleId, RoleStats> {
        val counts = mutableMapOf<RoleId, Int>()

        for (result in results) {
            for ((roleId, count) in result.roleAssignmentCounts) {
                counts[roleId] = (counts[roleId] ?: 0) + count
            }
        }

        return counts.map { (roleId, total) ->
            roleId to RoleStats(
                roleId = roleId,
                timesAssigned = total,
                avgEggsProduced = 0.0, // TODO: track from resolution context
                avgEggsGainedSelf = 0.0,
                stealAttempts = 0,
                stealSuccesses = 0,
                avgVisitorsWhenPassive = 0.0,
                investigationCount = 0,
                correctInvestigations = 0
            )
        }.toMap()
    }
}
