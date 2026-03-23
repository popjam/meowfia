package com.meowfia.app.bot

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.util.RandomProvider

/**
 * Single source of truth for bot night-action decisions.
 * Used by both the real game UI and the simulation engine.
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
        // Only attempt smart targeting if skill check passes
        if (random.nextFloat() < strategy.nightSkill) {
            val smartTarget = getSmartTarget(bot, candidates, random)
            if (smartTarget != null) return smartTarget
        }
        return candidates[random.nextInt(candidates.size)].id
    }

    /**
     * Role-aware targeting logic. Returns null if no special logic applies,
     * falling back to random selection.
     */
    private fun getSmartTarget(
        bot: Player,
        candidates: List<Player>,
        random: RandomProvider
    ): Int? {
        return when (bot.roleId) {
            // --- Investigation: target the opposite team ---
            RoleId.HAWK -> {
                // Hawk wants to find Meowfia to gain an egg
                pickFromAlignment(candidates, Alignment.MEOWFIA, random)
            }
            RoleId.OWL -> {
                // Owl wants to investigate suspicious players (opposite team)
                pickFromAlignment(candidates, oppositeOf(bot.alignment), random)
            }
            RoleId.HOUSE_CAT -> {
                // House Cat (Meowfia) scouts Farm players for intel
                pickFromAlignment(candidates, Alignment.FARM, random)
            }

            // --- Egg laying: target your own team ---
            RoleId.PIGEON, RoleId.CHICKEN -> {
                // Lay eggs in allies' nests to help your team's egg count
                pickFromAlignment(candidates, bot.alignment, random)
            }

            // --- Tracking: target players likely to have visitors ---
            RoleId.EAGLE -> {
                // Eagle gains eggs equal to visitor count — target popular players.
                // Heuristic: non-buffer roles attract more visits.
                val interesting = candidates.filter { !it.roleId.isBuffer }
                if (interesting.isNotEmpty()) interesting[random.nextInt(interesting.size)].id
                else null
            }
            RoleId.FALCON -> {
                // Falcon lays egg where target visited — target players who are likely visiting someone.
                // Heuristic: players with PickPlayer roles (not Turkey/BlackSwan/auto) are visiting.
                val visitors = candidates.filter { it.roleId !in STAY_HOME_ROLES }
                if (visitors.isNotEmpty()) visitors[random.nextInt(visitors.size)].id
                else null
            }

            // --- Swaps: role-specific strategy ---
            RoleId.FROG -> {
                // If Meowfia: swap with a Farm player to disguise yourself
                // If Farm: swap with a suspected Meowfia player to disrupt
                pickFromAlignment(candidates, oppositeOf(bot.alignment), random)
            }
            RoleId.SWITCHEROO -> {
                // Target a player who is visiting someone (so the swap triggers).
                // Prefer targeting opposite team to disrupt them.
                val opposites = candidates.filter { it.alignment != bot.alignment }
                val pool = if (opposites.isNotEmpty()) opposites else candidates
                val visitors = pool.filter { it.roleId !in STAY_HOME_ROLES }
                if (visitors.isNotEmpty()) visitors[random.nextInt(visitors.size)].id
                else pool[random.nextInt(pool.size)].id
            }
            RoleId.SHEEP -> {
                // Sheep adopts target's alignment — target opposite team only if you
                // want to switch sides (risky). Safe play: target your own team.
                pickFromAlignment(candidates, bot.alignment, random)
            }

            else -> null
        }
    }

    /** Pick a random player from the given alignment, or null if none match. */
    private fun pickFromAlignment(
        candidates: List<Player>,
        alignment: Alignment,
        random: RandomProvider
    ): Int? {
        val matches = candidates.filter { it.alignment == alignment }
        return if (matches.isNotEmpty()) matches[random.nextInt(matches.size)].id
        else null
    }

    private fun oppositeOf(alignment: Alignment): Alignment = when (alignment) {
        Alignment.FARM -> Alignment.MEOWFIA
        Alignment.MEOWFIA -> Alignment.FARM
    }

    /** Roles that stay home or self-visit (won't appear in visit graph as visitors). */
    private val STAY_HOME_ROLES = setOf(
        RoleId.TURKEY, RoleId.BLACK_SWAN, RoleId.MOSQUITO, RoleId.TIT
    )
}
