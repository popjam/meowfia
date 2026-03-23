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
        Player(id = 0, name = "Alice", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
        Player(id = 1, name = "Bot1", roleId = RoleId.PIGEON, isBot = true, alignment = Alignment.FARM),
        Player(id = 2, name = "Bot2", roleId = RoleId.HAWK, isBot = true, alignment = Alignment.FARM),
        Player(id = 3, name = "Bot3", roleId = RoleId.TURKEY, isBot = true, alignment = Alignment.MEOWFIA)
    )

    private val perfectSkill = BotStrategy(name = "Perfect", nightSkill = 1.0f)
    private val noSkill = BotStrategy(name = "NoSkill", nightSkill = 0.0f)

    // --- Basic prompt handling ---

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

    // --- Smart targeting with perfect skill ---

    @Test
    fun skilled_hawk_targets_meowfia() {
        val players = listOf(
            Player(id = 0, name = "Hawk", roleId = RoleId.HAWK, alignment = Alignment.FARM),
            Player(id = 1, name = "Farm1", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
            Player(id = 2, name = "Farm2", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
            Player(id = 3, name = "Meowfia1", roleId = RoleId.HOUSE_CAT, alignment = Alignment.MEOWFIA)
        )
        val hawk = players[0]

        // With perfect skill, should always target the Meowfia player
        repeat(50) { seed ->
            val action = BotBrain.chooseNightAction(hawk, players, RandomProvider(seed.toLong()), perfectSkill)
            assertThat(action).isInstanceOf(NightAction.VisitPlayer::class.java)
            assertThat((action as NightAction.VisitPlayer).targetId).isEqualTo(3)
        }
    }

    @Test
    fun skilled_pigeon_targets_farm_allies() {
        val players = listOf(
            Player(id = 0, name = "Pigeon", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
            Player(id = 1, name = "Meowfia1", roleId = RoleId.HOUSE_CAT, alignment = Alignment.MEOWFIA),
            Player(id = 2, name = "Meowfia2", roleId = RoleId.HOUSE_CAT, alignment = Alignment.MEOWFIA),
            Player(id = 3, name = "Farm1", roleId = RoleId.PIGEON, alignment = Alignment.FARM)
        )
        val pigeon = players[0]

        // With perfect skill, should always target the Farm ally
        repeat(50) { seed ->
            val action = BotBrain.chooseNightAction(pigeon, players, RandomProvider(seed.toLong()), perfectSkill)
            assertThat(action).isInstanceOf(NightAction.VisitPlayer::class.java)
            assertThat((action as NightAction.VisitPlayer).targetId).isEqualTo(3)
        }
    }

    @Test
    fun skilled_house_cat_targets_farm() {
        val players = listOf(
            Player(id = 0, name = "Cat", roleId = RoleId.HOUSE_CAT, alignment = Alignment.MEOWFIA),
            Player(id = 1, name = "Meowfia1", roleId = RoleId.HOUSE_CAT, alignment = Alignment.MEOWFIA),
            Player(id = 2, name = "Farm1", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
            Player(id = 3, name = "Farm2", roleId = RoleId.HAWK, alignment = Alignment.FARM)
        )
        val cat = players[0]

        // With perfect skill, should always target Farm players for intel
        repeat(50) { seed ->
            val action = BotBrain.chooseNightAction(cat, players, RandomProvider(seed.toLong()), perfectSkill)
            assertThat(action).isInstanceOf(NightAction.VisitPlayer::class.java)
            val targetId = (action as NightAction.VisitPlayer).targetId
            assertThat(targetId).isAnyOf(2, 3)
        }
    }

    @Test
    fun unskilled_bot_picks_randomly() {
        val players = listOf(
            Player(id = 0, name = "Hawk", roleId = RoleId.HAWK, alignment = Alignment.FARM),
            Player(id = 1, name = "Farm1", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
            Player(id = 2, name = "Farm2", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
            Player(id = 3, name = "Meowfia1", roleId = RoleId.HOUSE_CAT, alignment = Alignment.MEOWFIA)
        )
        val hawk = players[0]

        // With zero skill, should hit non-Meowfia targets sometimes
        val targets = (0 until 100).map { seed ->
            val action = BotBrain.chooseNightAction(hawk, players, RandomProvider(seed.toLong()), noSkill)
            (action as NightAction.VisitPlayer).targetId
        }.toSet()

        // Should have hit multiple different targets (not just Meowfia)
        assertThat(targets.size).isGreaterThan(1)
    }

    @Test
    fun default_strategy_works_without_explicit_strategy() {
        // Existing callers that don't pass a strategy should still work
        val bot = testPlayers[1]
        val action = BotBrain.chooseNightAction(bot, testPlayers, RandomProvider(42))
        assertThat(action).isInstanceOf(NightAction.VisitPlayer::class.java)
    }

    @Test
    fun skilled_frog_targets_opposite_team() {
        val players = listOf(
            Player(id = 0, name = "Frog", roleId = RoleId.FROG, alignment = Alignment.FARM),
            Player(id = 1, name = "Farm1", roleId = RoleId.PIGEON, alignment = Alignment.FARM),
            Player(id = 2, name = "Meowfia1", roleId = RoleId.HOUSE_CAT, alignment = Alignment.MEOWFIA),
            Player(id = 3, name = "Farm2", roleId = RoleId.HAWK, alignment = Alignment.FARM)
        )
        val frog = players[0]

        // Skilled Farm Frog targets Meowfia to disrupt
        repeat(50) { seed ->
            val action = BotBrain.chooseNightAction(frog, players, RandomProvider(seed.toLong()), perfectSkill)
            assertThat((action as NightAction.VisitPlayer).targetId).isEqualTo(2)
        }
    }
}
