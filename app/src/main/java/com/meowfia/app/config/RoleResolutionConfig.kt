package com.meowfia.app.config

import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId

/**
 * Standalone, versioned configuration for night resolution ordering.
 *
 * Tier 1 (10): Roleblocks / hugs — Koala, Sheepdog, Floofer
 * Tier 2 (20): Redirects / swaps — Top Cat
 * Tier 3 (30): Egg laying — Pigeon, Chicken, Mosquito, Tit, Magpie
 * Tier 4 (40): Steals — House Cat
 * Tier 5 (50): Investigation — Hawk, Owl, Blind Hawk, Kookaburra, Mouser
 * Tier 5.5 (55): Swaps — Frog, Switcheroo, Sheep
 * Tier 6 (60): Tracking / graph-dependent — Eagle, Falcon, Lovebird
 * Tier 7 (70): Passive / reactive — Turkey
 * Tier 8 (80): Self-visit — Black Swan
 */
object RoleResolutionConfig {
    const val VERSION = "1.2.0"

    private val defaultPriorities: Map<RoleId, Int> = mapOf(
        RoleId.KOALA to 10,
        RoleId.SHEEPDOG to 10,
        RoleId.FLOOFER to 10,
        RoleId.TOP_CAT to 20,
        RoleId.PIGEON to 30,
        RoleId.CHICKEN to 30,
        RoleId.MOSQUITO to 30,
        RoleId.TIT to 30,
        RoleId.MAGPIE to 30,
        RoleId.HOUSE_CAT to 40,
        RoleId.HAWK to 50,
        RoleId.OWL to 50,
        RoleId.BLIND_HAWK to 50,
        RoleId.KOOKABURRA to 50,
        RoleId.MOUSER to 50,
        RoleId.FROG to 55,
        RoleId.SWITCHEROO to 55,
        RoleId.SHEEP to 55,
        RoleId.EAGLE to 60,
        RoleId.FALCON to 60,
        RoleId.LOVEBIRD to 60,
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
