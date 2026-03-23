package com.meowfia.app.bot

import com.meowfia.app.util.RandomProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BotNamesTest {

    @Test
    fun pick_returns_correct_count() {
        val names = BotNames.pick(3, RandomProvider(42))
        assertThat(names).hasSize(3)
    }

    @Test
    fun all_names_contain_bot() {
        val names = BotNames.pick(8, RandomProvider(42))
        for (name in names) {
            assertThat(name.lowercase()).contains("bot")
        }
    }

    @Test
    fun names_are_unique() {
        val names = BotNames.pick(8, RandomProvider(42))
        assertThat(names.toSet()).hasSize(8)
    }

    @Test
    fun overflow_names_get_suffixes() {
        val names = BotNames.pick(20, RandomProvider(42))
        assertThat(names).hasSize(20)
        assertThat(names.toSet()).hasSize(20)
    }
}
