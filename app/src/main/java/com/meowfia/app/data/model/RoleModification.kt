package com.meowfia.app.data.model

/**
 * Ordered operations that modify player roles or alignments during night resolution.
 * Applied sequentially — each operation sees the result of all prior operations.
 */
sealed class RoleModification {
    /** Exchange current roles between two players. */
    data class SwapRoles(val playerIdA: Int, val playerIdB: Int) : RoleModification()

    /** Set a player's role to a specific value (e.g. copy, transform). */
    data class SetRole(val playerId: Int, val roleId: RoleId) : RoleModification()

    /** Set a player's alignment to a specific value. */
    data class SetAlignment(val playerId: Int, val alignment: Alignment) : RoleModification()
}
