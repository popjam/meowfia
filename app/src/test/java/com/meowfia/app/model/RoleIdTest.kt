package com.meowfia.app.model

import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RoleIdTest {

    @Test
    fun correct_number_of_implemented_animals() {
        val implementedAnimals = RoleId.entries.filter {
            it.implemented && (it.isFarmAnimal || it.isMeowfiaAnimal)
        }
        assertThat(implementedAnimals).hasSize(23) // 14 previous + BlindHawk, Lovebird, Kookaburra, Magpie, TopCat, Koala, Sheepdog, Mouser, Floofer
    }

    @Test
    fun correct_number_of_implemented_flowers() {
        val flowers = RoleId.entries.filter { it.implemented && it.isFlower }
        assertThat(flowers).hasSize(18) // 17 previous + Wildflower
    }

    @Test
    fun pigeon_is_buffer() {
        assertThat(RoleId.PIGEON.isBuffer).isTrue()
    }

    @Test
    fun house_cat_is_buffer() {
        assertThat(RoleId.HOUSE_CAT.isBuffer).isTrue()
    }

    @Test
    fun hawk_is_not_buffer() {
        assertThat(RoleId.HAWK.isBuffer).isFalse()
    }

    @Test
    fun future_roles_not_implemented() {
        assertThat(RoleId.CAT_BURGLER.implemented).isFalse()
        assertThat(RoleId.UGLY_DUCKLING.implemented).isFalse()
    }

    @Test
    fun card_types_correct() {
        assertThat(RoleId.PIGEON.cardType).isEqualTo(CardType.FARM_ANIMAL)
        assertThat(RoleId.HOUSE_CAT.cardType).isEqualTo(CardType.MEOWFIA_ANIMAL)
        assertThat(RoleId.SUNFLOWER.cardType).isEqualTo(CardType.FLOWER)
    }
}
