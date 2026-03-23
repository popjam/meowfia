package com.meowfia.app.engine

import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry

/**
 * Builds a DawnReport for each player from the ResolutionContext.
 */
class DawnReportGenerator {

    fun generate(
        players: List<Player>,
        context: ResolutionContext,
        activeFlowers: List<RoleId> = emptyList()
    ): Map<Int, DawnReport> {
        val baseReports = context.buildDawnReports()

        return players.associate { player ->
            val baseReport = baseReports[player.id] ?: DawnReport(
                playerId = player.id,
                reportedEggDelta = 0,
                actualEggDelta = 0,
                additionalInfo = emptyList()
            )

            // Add role-specific dawn info
            val handler = RoleRegistry.get(player.roleId)
            val roleInfo = handler.getDawnInfo(player, context)

            // Add flower-related info
            val flowerInfo = buildFlowerInfo(activeFlowers)

            val allInfo = baseReport.additionalInfo + roleInfo + flowerInfo

            player.id to baseReport.copy(additionalInfo = allInfo.distinct())
        }
    }

    private fun buildFlowerInfo(activeFlowers: List<RoleId>): List<String> {
        val info = mutableListOf<String>()
        if (RoleId.SUNFLOWER in activeFlowers) {
            info.add("Sunflower active: one player may reveal their role during day to gain 2 eggs.")
        }
        return info
    }
}
