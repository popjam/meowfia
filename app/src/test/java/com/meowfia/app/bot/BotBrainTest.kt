package com.meowfia.app.bot

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class BotBrainTest {

    @Before
    fun setup() {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
    }

    private val testPlayers = listOf(
        Player(id = 0, name = "Alice", roleId = RoleId.PIGEON),
        Player(id = 1, name = "Bot1", roleId = RoleId.PIGEON, isBot = true),
        Player(id = 2, name = "Bot2", roleId = RoleId.HAWK, isBot = true),
        Player(id = 3, name = "Bot3", roleId = RoleId.TURKEY, isBot = true)
    )

    @Test
    fun bot_picks_valid_target_for_pick_player_role() {
        val bot = testPlayers[1] // Pigeon — PickPlayer
        val action = BotBrain.chooseNightAction(bot, testPlayers, RandomProvider(42))
        assertThat(action).isInstanceOf(NightAction.VisitPlayer::class.java)
        val targetId = (action as NightAction.VisitPlayer).targetId
        assertThat(targetId).isNotEqualTo(bot.id)
    }

    @Test
    fun bot_returns_visit_random_for_automatic_role() {
        val bot = Player(id = 1, name = "TitBot", roleId = RoleId.TIT, isBot = true)
        val action = BotBrain.chooseNightAction(bot, testPlayers, RandomProvider(42))
        assertThat(action).isEqualTo(NightAction.VisitRandom)
    }

    @Test
    fun bot_returns_visit_self_for_self_visit_role() {
        val bot = Player(id = 1, name = "SwanBot", roleId = RoleId.BLACK_SWAN, isBot = true)
        val action = BotBrain.chooseNightAction(bot, testPlayers, RandomProvider(42))
        assertThat(action).isEqualTo(NightAction.VisitSelf)
    }

    @Test
    fun bot_never_picks_self_when_excluded() {
        val bot = testPlayers[2] // Hawk — PickPlayer with excludeSelf
        repeat(100) { seed ->
            val action = BotBrain.chooseNightAction(bot, testPlayers, RandomProvider(seed.toLong()))
            if (action is NightAction.VisitPlayer) {
                assertThat(action.targetId).isNotEqualTo(bot.id)
            }
        }
    }
}
