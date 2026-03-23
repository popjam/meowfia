package com.meowfia.app.engine

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.RoleModification
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class RoleModificationTest {

    @Before
    fun setup() {
        RoleRegistry.initialize()
        FlowerRegistry.initialize()
    }

    private fun makeContext(players: List<Player>, visitGraph: Map<Int, Int?> = emptyMap()) =
        ResolutionContext(players, emptyList(), visitGraph, RandomProvider(42))

    // --- ResolutionContext unit tests ---

    @Test
    fun swap_roles_exchanges_both_players() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1)

        assertThat(ctx.getCurrentRole(0)).isEqualTo(RoleId.PIGEON)
        assertThat(ctx.getCurrentRole(1)).isEqualTo(RoleId.FROG)
    }

    @Test
    fun multiple_swaps_chain_correctly() {
        // A=Frog, B=Pigeon, C=Hawk
        // Swap A<->B: A=Pigeon, B=Frog, C=Hawk
        // Swap B<->C: A=Pigeon, B=Hawk, C=Frog
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON),
            Player(2, "Charlie", roleId = RoleId.HAWK)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1)
        ctx.swapRoles(1, 2)

        assertThat(ctx.getCurrentRole(0)).isEqualTo(RoleId.PIGEON)
        assertThat(ctx.getCurrentRole(1)).isEqualTo(RoleId.HAWK)
        assertThat(ctx.getCurrentRole(2)).isEqualTo(RoleId.FROG)
    }

    @Test
    fun swap_then_swap_back_restores_original() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1)
        ctx.swapRoles(0, 1)

        assertThat(ctx.getCurrentRole(0)).isEqualTo(RoleId.FROG)
        assertThat(ctx.getCurrentRole(1)).isEqualTo(RoleId.PIGEON)
    }

    @Test
    fun three_way_circular_swap() {
        // A=Frog, B=Pigeon, C=Hawk
        // Swap A<->B: A=Pigeon, B=Frog, C=Hawk
        // Swap A<->C: A=Hawk, B=Frog, C=Pigeon
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON),
            Player(2, "Charlie", roleId = RoleId.HAWK)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1)
        ctx.swapRoles(0, 2)

        assertThat(ctx.getCurrentRole(0)).isEqualTo(RoleId.HAWK)
        assertThat(ctx.getCurrentRole(1)).isEqualTo(RoleId.FROG)
        assertThat(ctx.getCurrentRole(2)).isEqualTo(RoleId.PIGEON)
    }

    @Test
    fun set_role_overrides_current() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG)
        )
        val ctx = makeContext(players)

        ctx.setRole(0, RoleId.HAWK)

        assertThat(ctx.getCurrentRole(0)).isEqualTo(RoleId.HAWK)
    }

    @Test
    fun set_role_after_swap_uses_latest() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1) // A=Pigeon, B=Frog
        ctx.setRole(0, RoleId.HAWK) // A=Hawk, B=Frog

        assertThat(ctx.getCurrentRole(0)).isEqualTo(RoleId.HAWK)
        assertThat(ctx.getCurrentRole(1)).isEqualTo(RoleId.FROG)
    }

    @Test
    fun swap_after_set_role_uses_set_value() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON)
        )
        val ctx = makeContext(players)

        ctx.setRole(0, RoleId.HAWK) // A=Hawk, B=Pigeon
        ctx.swapRoles(0, 1)        // A=Pigeon, B=Hawk

        assertThat(ctx.getCurrentRole(0)).isEqualTo(RoleId.PIGEON)
        assertThat(ctx.getCurrentRole(1)).isEqualTo(RoleId.HAWK)
    }

    @Test
    fun set_alignment_works() {
        val players = listOf(
            Player(0, "Alice", alignment = Alignment.FARM)
        )
        val ctx = makeContext(players)

        ctx.setAlignment(0, Alignment.MEOWFIA)

        assertThat(ctx.getCurrentAlignment(0)).isEqualTo(Alignment.MEOWFIA)
    }

    @Test
    fun compute_final_roles_only_returns_changed() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON),
            Player(2, "Charlie", roleId = RoleId.HAWK)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1) // Only Alice and Bob change

        val finalRoles = ctx.computeFinalRoles()
        assertThat(finalRoles).hasSize(2)
        assertThat(finalRoles[0]).isEqualTo(RoleId.PIGEON)
        assertThat(finalRoles[1]).isEqualTo(RoleId.FROG)
        assertThat(finalRoles).doesNotContainKey(2)
    }

    @Test
    fun compute_final_roles_after_swap_back_returns_empty() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1)
        ctx.swapRoles(0, 1)

        val finalRoles = ctx.computeFinalRoles()
        assertThat(finalRoles).isEmpty()
    }

    @Test
    fun modifications_are_recorded_in_order() {
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON)
        )
        val ctx = makeContext(players)

        ctx.swapRoles(0, 1)
        ctx.setRole(0, RoleId.HAWK)
        ctx.setAlignment(0, Alignment.MEOWFIA)

        val mods = ctx.getModifications()
        assertThat(mods).hasSize(3)
        assertThat(mods[0]).isInstanceOf(RoleModification.SwapRoles::class.java)
        assertThat(mods[1]).isInstanceOf(RoleModification.SetRole::class.java)
        assertThat(mods[2]).isInstanceOf(RoleModification.SetAlignment::class.java)
    }

    // --- Integration: Frog + Switcheroo on same player ---

    @Test
    fun frog_then_switcheroo_on_overlapping_player_chains_correctly() {
        // Setup: 4 players
        // Alice=Frog visits Bob -> swap Alice<->Bob
        // Charlie=Switcheroo visits Bob -> Bob swaps with whoever Bob visited (Diana)
        // After Frog: Alice=Pigeon, Bob=Frog
        // After Switcheroo: Bob swaps with Diana -> Bob=Hawk, Diana=Frog
        // Final: Alice=Pigeon, Bob=Hawk, Charlie=Switcheroo, Diana=Frog
        val coord = GameCoordinator(RandomProvider(42))
        val pool = listOf(
            PoolCard(RoleId.FROG), PoolCard(RoleId.SWITCHEROO),
            PoolCard(RoleId.PIGEON), PoolCard(RoleId.HAWK)
        )
        coord.startNewRound(1, pool, listOf("Alice", "Bob", "Charlie", "Diana"), 0)
        coord.assignRoles(
            forcedAlignments = mapOf(
                0 to Alignment.FARM, 1 to Alignment.FARM,
                2 to Alignment.FARM, 3 to Alignment.FARM
            ),
            forcedRoles = mapOf(
                0 to RoleId.FROG, 1 to RoleId.PIGEON,
                2 to RoleId.SWITCHEROO, 3 to RoleId.HAWK
            )
        )

        // Alice(Frog) visits Bob, Bob(Pigeon) visits Diana,
        // Charlie(Switcheroo) visits Bob, Diana(Hawk) visits Alice
        coord.submitNightAction(0, NightAction.VisitPlayer(1)) // Frog -> Bob
        coord.submitNightAction(1, NightAction.VisitPlayer(3)) // Pigeon -> Diana
        coord.submitNightAction(2, NightAction.VisitPlayer(1)) // Switcheroo -> Bob
        coord.submitNightAction(3, NightAction.VisitPlayer(0)) // Hawk -> Alice

        coord.resolveNight()

        // After Frog (tier 55, seat 0): Alice=Pigeon, Bob=Frog
        // After Switcheroo (tier 55, seat 2): Bob(now Frog) swaps with Diana(Hawk)
        //   -> Bob=Hawk, Diana=Frog
        // Final: Alice=Pigeon, Bob=Hawk, Charlie=Switcheroo, Diana=Frog
        val alice = coord.state.players.find { it.name == "Alice" }!!
        val bob = coord.state.players.find { it.name == "Bob" }!!
        val charlie = coord.state.players.find { it.name == "Charlie" }!!
        val diana = coord.state.players.find { it.name == "Diana" }!!

        assertThat(alice.roleId).isEqualTo(RoleId.PIGEON)
        assertThat(bob.roleId).isEqualTo(RoleId.HAWK)
        assertThat(charlie.roleId).isEqualTo(RoleId.SWITCHEROO)
        assertThat(diana.roleId).isEqualTo(RoleId.FROG)
    }

    @Test
    fun black_swan_detects_swap_from_frog() {
        // Alice=Frog visits Bob=Black Swan -> swap: Alice=Black Swan, Bob=Frog
        // Black Swan (tier 80) checks: Bob's current role is Frog, no egg
        val coord = GameCoordinator(RandomProvider(42))
        val pool = listOf(
            PoolCard(RoleId.FROG), PoolCard(RoleId.BLACK_SWAN), PoolCard(RoleId.PIGEON)
        )
        coord.startNewRound(1, pool, listOf("Alice", "Bob", "Charlie", "Diana"), 0)
        coord.assignRoles(
            forcedAlignments = mapOf(
                0 to Alignment.FARM, 1 to Alignment.FARM,
                2 to Alignment.FARM, 3 to Alignment.FARM
            ),
            forcedRoles = mapOf(
                0 to RoleId.FROG, 1 to RoleId.BLACK_SWAN,
                2 to RoleId.PIGEON, 3 to RoleId.PIGEON
            )
        )

        coord.submitNightAction(0, NightAction.VisitPlayer(1)) // Frog -> Black Swan
        coord.submitNightAction(1, NightAction.VisitSelf)      // Black Swan visits self
        coord.submitNightAction(2, NightAction.VisitPlayer(0))
        coord.submitNightAction(3, NightAction.VisitPlayer(0))

        coord.resolveNight()

        // Bob was Black Swan but got swapped to Frog at tier 55
        // At tier 80, Black Swan handler runs for Bob but getCurrentRole returns Frog
        // So Bob should NOT get an egg
        val bobReport = coord.getDawnReport(1)
        assertThat(bobReport.actualEggDelta).isEqualTo(0)

        // Bob's dawn info should say "You are the Frog"
        assertThat(bobReport.additionalInfo).isNotEmpty()
        assertThat(bobReport.additionalInfo.any { it.contains("Frog") }).isTrue()
    }

    @Test
    fun no_role_duplication_with_overlapping_swaps() {
        // Verify that all roles are conserved (no duplication or loss)
        val players = listOf(
            Player(0, "Alice", roleId = RoleId.FROG),
            Player(1, "Bob", roleId = RoleId.PIGEON),
            Player(2, "Charlie", roleId = RoleId.HAWK),
            Player(3, "Diana", roleId = RoleId.OWL)
        )
        val ctx = makeContext(players)

        // Multiple overlapping swaps
        ctx.swapRoles(0, 1) // Frog<->Pigeon
        ctx.swapRoles(1, 2) // (now Frog)<->Hawk
        ctx.swapRoles(2, 3) // (now Frog)<->Owl

        val finalRoles = listOf(
            ctx.getCurrentRole(0),
            ctx.getCurrentRole(1),
            ctx.getCurrentRole(2),
            ctx.getCurrentRole(3)
        )

        // All original roles should still exist exactly once
        assertThat(finalRoles).containsExactly(
            RoleId.PIGEON, RoleId.HAWK, RoleId.OWL, RoleId.FROG
        )
    }
}
