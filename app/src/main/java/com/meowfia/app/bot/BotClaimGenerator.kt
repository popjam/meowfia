package com.meowfia.app.bot

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
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
        // Eggs this role generates for ITSELF — read from handler's declared range
        val handler = if (RoleRegistry.isRegistered(claimedRole)) RoleRegistry.get(claimedRole) else null
        val selfRange = handler?.getSelfEggRange() ?: (0..0)
        val selfEggs = random.nextInt(selfRange.first, selfRange.last + 1)

        // Estimate eggs received from other roles visiting you.
        // Each egg-laying role visits one player out of (playerCount - 1).
        val targets = maxOf(1, playerCount - 1)
        var expectedReceived = 0f
        for (role in pool) {
            val roleHandler = if (RoleRegistry.isRegistered(role)) RoleRegistry.get(role) else null
            // Heuristic: if a role has target-egg behavior, it contributes eggs to the visited player.
            // We estimate based on the role's description keywords since target eggs aren't in getSelfEggRange.
            val eggsPerVisit = estimateTargetEggs(role)
            expectedReceived += eggsPerVisit / targets
        }

        // Base estimate + random variation of ±1
        val base = selfEggs + expectedReceived.toInt()
        val variation = random.nextInt(-1, 2)  // -1, 0, or +1
        return maxOf(0, base + variation)  // Egg deltas below 0 are rare, keep it non-negative
    }

    /**
     * Estimates how many eggs a role lays in its TARGET's nest per visit.
     * This is separate from self-eggs (handled by getSelfEggRange).
     * Uses the role description as a heuristic — new roles that mention
     * "lay X eggs" in their description will be roughly estimated.
     */
    private fun estimateTargetEggs(role: RoleId): Float {
        // Check description for egg-laying patterns
        val desc = role.description.lowercase()
        return when {
            desc.contains("lay 2 eggs") -> 2f
            desc.contains("lay an egg") || desc.contains("lay 1 egg") -> 1f
            desc.contains("lay") && desc.contains("egg") -> 1f
            else -> 0f
        }
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
