package com.meowfia.app.bot

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.util.RandomProvider

object BotClaimGenerator {

    fun generateClaim(
        bot: Player,
        allPlayers: List<Player>,
        dawnReport: DawnReport,
        visitGraph: Map<Int, Int?>,
        pool: List<RoleId>,
        random: RandomProvider
    ): BotDayClaim {
        return if (bot.alignment == Alignment.FARM) {
            generateTruthfulClaim(bot, allPlayers, dawnReport, visitGraph)
        } else {
            generateLie(bot, allPlayers, dawnReport, pool, random)
        }
    }

    private fun generateTruthfulClaim(
        bot: Player,
        allPlayers: List<Player>,
        dawnReport: DawnReport,
        visitGraph: Map<Int, Int?>
    ): BotDayClaim {
        val targetId = visitGraph[bot.id]
        val targetName = getTargetClaimText(bot.roleId, targetId, allPlayers)

        return BotDayClaim(
            playerId = bot.id,
            botName = bot.name,
            claimedRole = bot.roleId,
            claimedTargetName = targetName,
            claimedEggDelta = dawnReport.reportedEggDelta,
            isLying = false
        )
    }

    private fun generateLie(
        bot: Player,
        allPlayers: List<Player>,
        dawnReport: DawnReport,
        pool: List<RoleId>,
        random: RandomProvider
    ): BotDayClaim {
        val farmRolesInPool = pool.filter { it.isFarmAnimal && it != RoleId.PIGEON }
        val claimedRole = if (farmRolesInPool.isNotEmpty()) {
            farmRolesInPool[random.nextInt(farmRolesInPool.size)]
        } else {
            RoleId.PIGEON
        }

        val others = allPlayers.filter { it.id != bot.id }
        val fakeTarget = others[random.nextInt(others.size)]
        val targetName = getTargetClaimText(claimedRole, fakeTarget.id, allPlayers)

        val plausibleDelta = random.nextInt(-1, 3)

        return BotDayClaim(
            playerId = bot.id,
            botName = bot.name,
            claimedRole = claimedRole,
            claimedTargetName = targetName,
            claimedEggDelta = plausibleDelta,
            isLying = true
        )
    }

    private fun getTargetClaimText(
        roleId: RoleId,
        targetId: Int?,
        allPlayers: List<Player>
    ): String? {
        return when (roleId) {
            RoleId.TIT -> "a random Meowfia player"
            RoleId.MOSQUITO -> "a random player"
            RoleId.BLACK_SWAN -> "myself"
            RoleId.TURKEY -> null
            else -> targetId?.let { id ->
                allPlayers.find { it.id == id }?.name
            } ?: "someone"
        }
    }
}
