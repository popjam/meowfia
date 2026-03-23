package com.meowfia.app.config

import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId

/**
 * Standalone, versioned configuration for night resolution ordering.
 *
 * Tier 1 (10): Roleblocks / hugs (no v1 roles)
 * Tier 2 (20): Redirects / swaps (no v1 roles)
 * Tier 3 (30): Egg laying — Pigeon, Chicken, Mosquito, Tit
 * Tier 4 (40): Steals — House Cat
 * Tier 5 (50): Investigation — Hawk, Owl
 * Tier 6 (60): Tracking / graph-dependent — Eagle, Falcon
 * Tier 7 (70): Passive / reactive — Turkey
 * Tier 8 (80): Self-visit — Black Swan
 */
object RoleResolutionConfig {
    const val VERSION = "1.1.0"

    private val defaultPriorities: Map<RoleId, Int> = mapOf(
        RoleId.PIGEON to 30,
        RoleId.CHICKEN to 30,
        RoleId.MOSQUITO to 30,
        RoleId.TIT to 30,
        RoleId.HOUSE_CAT to 40,
        RoleId.HAWK to 50,
        RoleId.OWL to 50,
        RoleId.EAGLE to 60,
        RoleId.FALCON to 60,
        RoleId.TURKEY to 70,
        RoleId.BLACK_SWAN to 80,
    )

    private var overrides: Map<RoleId, Int> = emptyMap()

    fun getPriority(roleId: RoleId): Int =
        overrides[roleId]
            ?: defaultPriorities[roleId]
            ?: error("No resolution priority for $roleId — add it to RoleResolutionConfig")

    fun setOverrides(overrides: Map<RoleId, Int>) {
        this.overrides = overrides
    }

    fun clearOverrides() {
        overrides = emptyMap()
    }

    /** Returns players sorted by tier priority, then seat order clockwise from dealer. */
    fun getResolutionOrder(players: List<Player>, dealerSeat: Int): List<Player> {
        val n = players.size
        return players.sortedWith(
            compareBy<Player> { getPriority(it.roleId) }
                .thenBy { (it.id - dealerSeat + n) % n }
        )
    }

    /** Dandelion override: strict seat order from dealer, ignoring tiers. */
    fun getSeatOrder(players: List<Player>, dealerSeat: Int): List<Player> {
        val n = players.size
        return (0 until n).map { offset -> players[(dealerSeat + offset) % n] }
    }
}
