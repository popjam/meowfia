package com.meowfia.app.engine

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.util.RandomProvider

/**
 * Assigns alignments and roles to players each round.
 *
 * Algorithm:
 * 1. Roll alignment for each player: independent 1/3 Meowfia, 2/3 Farm.
 * 2. Collect non-buffer roles from pool, split by alignment type.
 * 3. Shuffle both role lists.
 * 4. Assign non-buffer roles first (one per player if available).
 * 5. Overflow gets Pigeon (Farm) or House Cat (Meowfia).
 */
class RoleAssigner(private val random: RandomProvider) {

    fun assign(
        playerCount: Int,
        pool: List<PoolCard>,
        forcedAlignments: Map<Int, Alignment>? = null,
        forcedRoles: Map<Int, RoleId>? = null,
        activeFlowers: List<RoleId> = emptyList()
    ): List<PlayerAssignment> {
        // Step 1: Roll alignments
        val meowfiaChance = if (RoleId.WOLFSBANE in activeFlowers) 1f / 2f else 1f / 3f
        val alignments = (0 until playerCount).map { playerId ->
            forcedAlignments?.get(playerId) ?: if (random.nextFloat() < meowfiaChance) {
                Alignment.MEOWFIA
            } else {
                Alignment.FARM
            }
        }

        // Step 2: Collect non-buffer roles from pool by type
        val farmRoles = random.shuffle(
            pool.filter { it.cardType == CardType.FARM_ANIMAL && !it.roleId.isBuffer }
                .map { it.roleId }
        ).toMutableList()

        val meowfiaRoles = random.shuffle(
            pool.filter { it.cardType == CardType.MEOWFIA_ANIMAL && !it.roleId.isBuffer }
                .map { it.roleId }
        ).toMutableList()

        // Step 3: Assign roles
        val assignments = mutableListOf<PlayerAssignment>()

        for (playerId in 0 until playerCount) {
            val forced = forcedRoles?.get(playerId)
            if (forced != null) {
                assignments.add(PlayerAssignment(playerId, alignments[playerId], forced))
                // Remove from available pools
                farmRoles.remove(forced)
                meowfiaRoles.remove(forced)
                continue
            }

            val alignment = alignments[playerId]
            val twinflowerActive = RoleId.TWINFLOWER in activeFlowers
            val role = when (alignment) {
                Alignment.FARM -> if (twinflowerActive && farmRoles.isNotEmpty()) {
                    farmRoles[random.nextInt(farmRoles.size)]
                } else {
                    farmRoles.removeFirstOrNull() ?: RoleId.PIGEON
                }
                Alignment.MEOWFIA -> if (twinflowerActive && meowfiaRoles.isNotEmpty()) {
                    meowfiaRoles[random.nextInt(meowfiaRoles.size)]
                } else {
                    meowfiaRoles.removeFirstOrNull() ?: RoleId.HOUSE_CAT
                }
            }
            assignments.add(PlayerAssignment(playerId, alignment, role))
        }

        return assignments
    }
}
