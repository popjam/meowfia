package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId

/**
 * Extended player state for simulation. Wraps the real [Player] and adds
 * simulation-only fields (hand, score pile, strategy).
 */
data class SimPlayer(
    var player: Player,
    val strategy: SimStrategy,
    val hand: MutableList<SimCard> = mutableListOf(),
    val scorePile: MutableList<SimCard> = mutableListOf(),
    var isDead: Boolean = false,
    var bonusPoints: Int = 0
) {
    val id get() = player.id
    val name get() = player.name
    val alignment get() = player.alignment
    val roleId get() = player.roleId

    fun totalScore(): Int = scorePile.sumOf { it.value } + hand.size + bonusPoints

    fun scorePileValue(): Int = scorePile.sumOf { it.value }

    /** Sync wrapper Player from engine assignments. */
    fun updateFromAssignment(alignment: Alignment, roleId: RoleId) {
        player = player.copy(alignment = alignment, roleId = roleId, originalRoleId = roleId)
    }
}
