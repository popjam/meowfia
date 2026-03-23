package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId

data class SimConfig(
    val seed: Long? = null,
    val playerCount: Int = 6,
    val roundCount: Int = 5,
    val playerNames: List<String>? = null,
    val strategies: List<SimStrategy>? = null,
    val forcedPool: List<RoleId>? = null,
    val forcedAlignments: Map<Int, Alignment>? = null,
    val forcedRoles: Map<Int, RoleId>? = null,
    val forcedVisits: Map<Int, Int>? = null,
    val includeFlowers: Boolean = false,
    val activeFlowerOverride: List<RoleId>? = null,
    val verbosity: Verbosity = Verbosity.FULL
) {
    companion object {
        val DEFAULT_NAMES = listOf(
            "Alice", "Bob", "Charlie", "Diana", "Eve", "Frank", "Grace", "Hank"
        )

        fun minimal(playerCount: Int = 6, seed: Long = 42) = SimConfig(
            seed = seed,
            playerCount = playerCount,
            roundCount = 1,
            verbosity = Verbosity.MINIMAL
        )
    }
}

enum class Verbosity {
    MINIMAL,
    SUMMARY,
    FULL,
    DEBUG
}
