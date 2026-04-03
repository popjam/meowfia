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

    /**
     * Bot targeting hint: which alignment should a smart bot prefer to target?
     * Defaults to [TargetPreference.RANDOM]. Override to provide role-specific targeting.
     * This is called by [com.meowfia.app.bot.BotBrain] — new roles get smart bot
     * behavior by overriding this method, with no changes to BotBrain itself.
     */
    fun getTargetPreference(actor: Player): TargetPreference = TargetPreference.RANDOM

    /**
     * Range of egg deltas this role can plausibly produce for ITSELF in one night
     * (not counting eggs received from other visitors).
     * Used by [com.meowfia.app.bot.BotClaimGenerator] for plausible lying.
     * Defaults to 0..0 (most roles don't give eggs to self).
     */
    fun getSelfEggRange(): IntRange = 0..0

    /**
     * Returns the list of valid visit targets for this role, derived from [getNightPrompt].
     * Used by [com.meowfia.app.testing.sim.RoundSolver] to enumerate possible worlds
     * without duplicating targeting logic.
     *
     * - [NightPrompt.PickPlayer]: all other players (or including self if `!excludeSelf`)
     * - [NightPrompt.SelfVisit]: only the player themselves
     * - [NightPrompt.Automatic]: all other players (target is chosen randomly at runtime)
     *
     * Override for roles with special targeting (e.g. Tit visits only Meowfia).
     */
    fun getValidTargets(player: Player, allPlayers: List<Player>): List<Player> {
        return when (val prompt = getNightPrompt(player, allPlayers)) {
            is NightPrompt.PickPlayer -> {
                if (prompt.excludeSelf) allPlayers.filter { it.id != player.id }
                else allPlayers
            }
            is NightPrompt.SelfVisit -> listOf(player)
            is NightPrompt.Automatic -> allPlayers.filter { it.id != player.id }
        }
    }
}
