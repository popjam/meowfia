package com.meowfia.app.testing

import com.meowfia.app.config.RoleResolutionConfig
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import org.junit.Test
import com.google.common.truth.Truth.assertThat

class ResolutionOrderTest : SimTestBase() {

    @Test
    fun pigeon_tier_before_house_cat_tier() {
        val pigeonTier = RoleResolutionConfig.getPriority(RoleId.PIGEON)
        val catTier = RoleResolutionConfig.getPriority(RoleId.HOUSE_CAT)
        assertThat(pigeonTier).isLessThan(catTier)
    }

    @Test
    fun hawk_tier_before_eagle_tier() {
        val hawkTier = RoleResolutionConfig.getPriority(RoleId.HAWK)
        val eagleTier = RoleResolutionConfig.getPriority(RoleId.EAGLE)
        assertThat(hawkTier).isLessThan(eagleTier)
    }

    @Test
    fun turkey_is_passive_tier() {
        val turkeyTier = RoleResolutionConfig.getPriority(RoleId.TURKEY)
        assertThat(turkeyTier).isEqualTo(70)
    }

    @Test
    fun same_tier_resolves_by_seat_order() {
        val players = listOf(
            Player(id = 0, name = "A", roleId = RoleId.PIGEON),
            Player(id = 1, name = "B", roleId = RoleId.CHICKEN),
            Player(id = 2, name = "C", roleId = RoleId.HAWK),
            Player(id = 3, name = "D", roleId = RoleId.MOSQUITO)
        )

        val order = RoleResolutionConfig.getResolutionOrder(players, dealerSeat = 0)
        val ids = order.map { it.id }

        // Tier 30 (Pigeon=0, Chicken=1, Mosquito=3) should come before Tier 50 (Hawk=2)
        assertThat(ids.indexOf(2)).isGreaterThan(ids.indexOf(0))
        assertThat(ids.indexOf(2)).isGreaterThan(ids.indexOf(1))
        assertThat(ids.indexOf(2)).isGreaterThan(ids.indexOf(3))
    }

    @Test
    fun seat_order_from_dealer() {
        val players = (0..5).map { Player(id = it, name = "P$it", roleId = RoleId.PIGEON) }
        val order = RoleResolutionConfig.getSeatOrder(players, dealerSeat = 3)
        assertThat(order.map { it.id }).isEqualTo(listOf(3, 4, 5, 0, 1, 2))
    }
}
