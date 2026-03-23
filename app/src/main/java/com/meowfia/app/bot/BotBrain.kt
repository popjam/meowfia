package com.meowfia.app.bot

import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.util.RandomProvider

object BotBrain {

    fun chooseNightAction(
        bot: Player,
        allPlayers: List<Player>,
        random: RandomProvider
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
                else NightAction.VisitPlayer(candidates[random.nextInt(candidates.size)].id)
            }
        }
    }
}
