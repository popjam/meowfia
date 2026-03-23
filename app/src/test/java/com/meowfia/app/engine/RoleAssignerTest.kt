package com.meowfia.app.engine

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoleAssignerTest {

    @Test
    fun assigns_correct_number_of_players() {
        val assigner = RoleAssigner(RandomProvider(42))
        val pool = listOf(
            PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT), PoolCard(RoleId.HAWK)
        )
        val assignments = assigner.assign(6, pool)
        assertThat(assignments).hasSize(6)
    }

    @Test
    fun all_player_ids_present() {
        val assigner = RoleAssigner(RandomProvider(42))
        val pool = listOf(
            PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT), PoolCard(RoleId.HAWK)
        )
        val assignments = assigner.assign(6, pool)
        assertThat(assignments.map { it.playerId }).containsExactly(0, 1, 2, 3, 4, 5)
    }

    @Test
    fun forced_alignments_respected() {
        val assigner = RoleAssigner(RandomProvider(42))
        val pool = listOf(PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT))
        val forced = mapOf(0 to Alignment.MEOWFIA, 1 to Alignment.FARM)
        val assignments = assigner.assign(4, pool, forcedAlignments = forced)

        assertThat(assignments.find { it.playerId == 0 }!!.alignment).isEqualTo(Alignment.MEOWFIA)
        assertThat(assignments.find { it.playerId == 1 }!!.alignment).isEqualTo(Alignment.FARM)
    }

    @Test
    fun forced_roles_respected() {
        val assigner = RoleAssigner(RandomProvider(42))
        val pool = listOf(PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT), PoolCard(RoleId.HAWK))
        val forcedRoles = mapOf(0 to RoleId.HAWK)
        val assignments = assigner.assign(4, pool, forcedRoles = forcedRoles)

        assertThat(assignments.find { it.playerId == 0 }!!.roleId).isEqualTo(RoleId.HAWK)
    }

    @Test
    fun overflow_farm_gets_pigeon() {
        val assigner = RoleAssigner(RandomProvider(42))
        val pool = listOf(PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT))
        val forced = (0..5).associateWith { Alignment.FARM }
        val assignments = assigner.assign(6, pool, forcedAlignments = forced)

        // All Farm with no non-buffer farm roles → all should be Pigeon
        assertThat(assignments.all { it.roleId == RoleId.PIGEON }).isTrue()
    }

    @Test
    fun overflow_meowfia_gets_house_cat() {
        val assigner = RoleAssigner(RandomProvider(42))
        val pool = listOf(PoolCard(RoleId.PIGEON), PoolCard(RoleId.HOUSE_CAT))
        val forced = (0..5).associateWith { Alignment.MEOWFIA }
        val assignments = assigner.assign(6, pool, forcedAlignments = forced)

        assertThat(assignments.all { it.roleId == RoleId.HOUSE_CAT }).isTrue()
    }
}
