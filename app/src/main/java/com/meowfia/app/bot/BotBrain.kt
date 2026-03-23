package com.meowfia.app.bot

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.roles.TargetPreference
import com.meowfia.app.util.RandomProvider

/**
 * Single source of truth for bot night-action decisions.
 * Used by both the real game UI and the simulation engine.
 *
 * Targeting logic is driven by each role's [TargetPreference] —
 * adding a new role with a custom preference automatically gets
 * smart bot behavior with no changes here.
 */
object BotBrain {

    fun chooseNightAction(
        bot: Player,
        allPlayers: List<Player>,
        random: RandomProvider,
        strategy: BotStrategy = BotStrategy.DEFAULT
    ): NightAction {
        val handler = RoleRegistry.get(bot.roleId)
        val prompt = handler.getNightPrompt(bot, allPlayers)

        return when (prompt) {
            is NightPrompt.Automatic -> NightAction.VisitRandom
            is NightPrompt.SelfVisit -> NightAction.VisitSelf
            is NightPrompt.PickPlayer -> {
                val candidates = if (prompt.excludeSelf) {
                    allPlayers.filter { it.id != bot.id }
                } else {
                    allPlayers
                }
                if (candidates.isEmpty()) NightAction.NoVisit
                else NightAction.VisitPlayer(
                    chooseTarget(bot, candidates, random, strategy)
                )
            }
        }
    }

    private fun chooseTarget(
        bot: Player,
        candidates: List<Player>,
        random: RandomProvider,
        strategy: BotStrategy
    ): Int {
        if (random.nextFloat() < strategy.nightSkill) {
            val handler = RoleRegistry.get(bot.roleId)
            val preference = handler.getTargetPreference(bot)
            val smartTarget = applyPreference(bot, candidates, preference, random)
            if (smartTarget != null) return smartTarget
        }
        return candidates[random.nextInt(candidates.size)].id
    }

    /**
     * Applies a [TargetPreference] to select a target from candidates.
     * Returns null if no preference-matching candidates exist (falls back to random).
     */
    private fun applyPreference(
        bot: Player,
        candidates: List<Player>,
        preference: TargetPreference,
        random: RandomProvider
    ): Int? {
        val filtered = when (preference) {
            TargetPreference.RANDOM -> return null
            TargetPreference.OPPOSITE_TEAM ->
                candidates.filter { it.alignment != bot.alignment }
            TargetPreference.SAME_TEAM ->
                candidates.filter { it.alignment == bot.alignment }
            TargetPreference.INTERESTING_ROLES ->
                candidates.filter { !it.roleId.isBuffer }
            TargetPreference.ACTIVE_VISITORS ->
                candidates.filter { it.roleId !in STAY_HOME_ROLES }
        }
        return if (filtered.isNotEmpty()) filtered[random.nextInt(filtered.size)].id else null
    }

    /** Roles that stay home or self-visit (won't appear in visit graph as visitors). */
    private val STAY_HOME_ROLES = setOf(
        RoleId.TURKEY, RoleId.BLACK_SWAN, RoleId.MOSQUITO, RoleId.TIT
    )
}
