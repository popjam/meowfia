package com.meowfia.app.bot

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BotClaimGeneratorTest {

    private val players = listOf(
        Player(id = 0, name = "Alice", alignment = Alignment.FARM, roleId = RoleId.PIGEON),
        Player(id = 1, name = "Botsworth", alignment = Alignment.FARM, roleId = RoleId.HAWK, isBot = true),
        Player(id = 2, name = "Meowbot", alignment = Alignment.MEOWFIA, roleId = RoleId.HOUSE_CAT, isBot = true),
        Player(id = 3, name = "Diana", alignment = Alignment.FARM, roleId = RoleId.OWL)
    )

    private val pool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK, RoleId.OWL)

    @Test
    fun farm_bot_claims_truthfully() {
        val bot = players[1] // Farm Hawk
        val report = DawnReport(playerId = 1, reportedEggDelta = 1, actualEggDelta = 1, additionalInfo = emptyList())
        val visitGraph = mapOf(1 to 2)

        val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, pool, RandomProvider(42))
        assertThat(claim.claimedRole).isEqualTo(RoleId.HAWK)
        assertThat(claim.claimedTargetName).isEqualTo("Meowbot")
        assertThat(claim.claimedEggDelta).isEqualTo(1)
        assertThat(claim.isLying).isFalse()
    }

    @Test
    fun meowfia_bot_claims_farm_role() {
        val bot = players[2] // Meowfia House Cat
        val report = DawnReport(playerId = 2, reportedEggDelta = 0, actualEggDelta = 0, additionalInfo = emptyList())
        val visitGraph = mapOf(2 to 0)

        repeat(50) { seed ->
            val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, pool, RandomProvider(seed.toLong()))
            assertThat(claim.claimedRole.isFarmAnimal).isTrue()
            assertThat(claim.isLying).isTrue()
        }
    }

    @Test
    fun meowfia_bot_egg_delta_is_plausible() {
        val bot = players[2]
        val report = DawnReport(playerId = 2, reportedEggDelta = 0, actualEggDelta = 0, additionalInfo = emptyList())
        val visitGraph = mapOf(2 to 0)

        repeat(100) { seed ->
            val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, pool, RandomProvider(seed.toLong()))
            // With a small pool (Pigeon, House Cat, Hawk, Owl) and 4 players,
            // plausible deltas should be non-negative and modest (0-2 typically)
            assertThat(claim.claimedEggDelta).isAtLeast(0)
            assertThat(claim.claimedEggDelta).isAtMost(4)
        }
    }

    @Test
    fun meowfia_bot_egg_delta_higher_with_more_layers_in_pool() {
        val bot = players[2]
        val report = DawnReport(playerId = 2, reportedEggDelta = 0, actualEggDelta = 0, additionalInfo = emptyList())
        val visitGraph = mapOf(2 to 0)

        // Pool with lots of egg-laying roles
        val bigPool = listOf(RoleId.PIGEON, RoleId.CHICKEN, RoleId.PIGEON, RoleId.MOSQUITO, RoleId.OWL)

        val deltas = (0 until 100).map { seed ->
            val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, bigPool, RandomProvider(seed.toLong()))
            claim.claimedEggDelta
        }
        val avgDelta = deltas.average()

        // With many layers in pool, average delta should be higher than with sparse pool
        val sparsePool = listOf(RoleId.HOUSE_CAT, RoleId.HAWK)
        val sparseDeltas = (0 until 100).map { seed ->
            val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, sparsePool, RandomProvider(seed.toLong()))
            claim.claimedEggDelta
        }
        val avgSparseDelta = sparseDeltas.average()

        assertThat(avgDelta).isGreaterThan(avgSparseDelta)
    }

    @Test
    fun turkey_bot_claims_stayed_home() {
        val bot = Player(id = 1, name = "TurkeyBot", alignment = Alignment.FARM, roleId = RoleId.TURKEY, isBot = true)
        val report = DawnReport(playerId = 1, reportedEggDelta = 0, actualEggDelta = 0, additionalInfo = emptyList())
        val visitGraph = mapOf<Int, Int?>(1 to null)

        val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, pool, RandomProvider(42))
        assertThat(claim.claimedTargetName).isNull()
    }

    @Test
    fun black_swan_bot_claims_self_visit() {
        val bot = Player(id = 1, name = "SwanBot", alignment = Alignment.FARM, roleId = RoleId.BLACK_SWAN, isBot = true)
        val report = DawnReport(playerId = 1, reportedEggDelta = 1, actualEggDelta = 1, additionalInfo = emptyList())
        val visitGraph = mapOf(1 to 1)

        val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, pool, RandomProvider(42))
        assertThat(claim.claimedTargetName).isEqualTo("myself")
    }

    @Test
    fun hawk_claim_never_exceeds_plausible_range() {
        // A Meowfia bot claiming Hawk should claim 0-2 eggs typically
        // (0 or 1 from Hawk ability + maybe 1 from a visitor)
        val bot = players[2]
        val report = DawnReport(playerId = 2, reportedEggDelta = 0, actualEggDelta = 0, additionalInfo = emptyList())
        val visitGraph = mapOf(2 to 0)
        // Pool with only Hawk (no layers to give received eggs)
        val hawkPool = listOf(RoleId.HAWK)

        repeat(100) { seed ->
            val claim = BotClaimGenerator.generateClaim(bot, players, report, visitGraph, hawkPool, RandomProvider(seed.toLong()))
            // With no egg-layers in pool, Hawk self-eggs are 0 or 1, variation ±1, clamped to ≥0
            assertThat(claim.claimedEggDelta).isAtMost(2)
        }
    }
}
