package com.meowfia.app.roles

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.ResolutionContext

/**
 * Interface that every role must implement.
 * Adding a new role means registering a new class — never modifying the engine.
 */
interface RoleHandler {
    val roleId: RoleId

    /** Returns the UI prompt for the night phase. */
    fun getNightPrompt(player: Player, allPlayers: List<Player>): NightPrompt

    /** Resolves the night action. Called in priority order by NightResolver. */
    fun resolve(
        actor: Player,
        target: Player?,
        gameState: GameState,
        context: ResolutionContext
    )

    /** Returns additional info lines for the dawn report. */
    fun getDawnInfo(player: Player, context: ResolutionContext): List<String> = emptyList()

    /** Returns how this player's alignment appears to investigators. */
    fun getApparentAlignment(player: Player): Alignment = player.alignment

    /** Returns how this player's role appears to investigators. */
    fun getApparentRole(player: Player): RoleId = player.roleId
}
