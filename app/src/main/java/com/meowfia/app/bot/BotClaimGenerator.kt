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

        val plausibleDelta = estimatePlausibleDelta(claimedRole, pool, allPlayers.size, random)

        return BotDayClaim(
            playerId = bot.id,
            botName = bot.name,
            claimedRole = claimedRole,
            claimedTargetName = targetName,
            claimedEggDelta = plausibleDelta,
            isLying = true
        )
    }

    /**
     * Estimates a plausible egg delta based on what the claimed role could produce
     * plus what visitors from the pool might contribute.
     */
    private fun estimatePlausibleDelta(
        claimedRole: RoleId,
        pool: List<RoleId>,
        playerCount: Int,
        random: RandomProvider
    ): Int {
        // Eggs this role generates for ITSELF (not for targets)
        val selfEggs = when (claimedRole) {
            RoleId.HAWK -> if (random.nextFloat() < 0.4f) 1 else 0  // ~40% chance of finding Meowfia
            RoleId.BLACK_SWAN -> 1  // Usually still Black Swan
            RoleId.EAGLE -> random.nextInt(0, 3)  // Depends on visitor count
            else -> 0  // Most roles lay eggs in OTHER nests, not their own
        }

        // Estimate eggs received from other roles visiting you.
        // Each egg-laying role visits one player out of (playerCount - 1).
        // Chance of being visited by any given layer = 1 / (playerCount - 1).
        val targets = maxOf(1, playerCount - 1)
        var expectedReceived = 0f
        for (role in pool) {
            val eggsPerVisit = when (role) {
                RoleId.CHICKEN -> 2f
                RoleId.PIGEON, RoleId.MOSQUITO, RoleId.TIT, RoleId.FALCON -> 1f
                RoleId.OWL -> 0.5f  // Only lays egg if target has no other visitors
                else -> 0f
            }
            expectedReceived += eggsPerVisit / targets
        }

        // Base estimate + random variation of ±1
        val base = selfEggs + expectedReceived.toInt()
        val variation = random.nextInt(-1, 2)  // -1, 0, or +1
        return maxOf(0, base + variation)  // Egg deltas below 0 are rare, keep it non-negative
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
