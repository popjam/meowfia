package com.meowfia.app.testing

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimEngine
import com.meowfia.app.testing.sim.Verbosity
import org.junit.Test

class EdgeCaseStressTest : SimTestBase() {

    @Test
    fun zero_meowfia_many_rounds() {
        repeat(100) { i ->
            SimEngine(SimConfig(
                seed = i.toLong(),
                playerCount = 6,
                roundCount = 1,
                forcedAlignments = (0..5).associateWith { Alignment.FARM },
                verbosity = Verbosity.MINIMAL
            )).runGame()
        }
        println("100 zero-Meowfia games passed")
    }

    @Test
    fun all_meowfia_many_rounds() {
        repeat(100) { i ->
            SimEngine(SimConfig(
                seed = i.toLong(),
                playerCount = 6,
                roundCount = 1,
                forcedAlignments = (0..5).associateWith { Alignment.MEOWFIA },
                verbosity = Verbosity.MINIMAL
            )).runGame()
        }
        println("100 all-Meowfia games passed")
    }

    @Test
    fun max_players_max_rounds() {
        repeat(50) { i ->
            SimEngine(SimConfig(
                seed = i.toLong(),
                playerCount = 8,
                roundCount = 8,
                verbosity = Verbosity.MINIMAL
            )).runGame()
        }
        println("50 max-size games (8 players, 8 rounds) passed")
    }

    @Test
    fun min_players_min_rounds() {
        repeat(100) { i ->
            SimEngine(SimConfig(
                seed = i.toLong(),
                playerCount = 4,
                roundCount = 1,
                verbosity = Verbosity.MINIMAL
            )).runGame()
        }
        println("100 min-size games (4 players, 1 round) passed")
    }

    @Test
    fun empty_pool_buffers_only() {
        repeat(50) { i ->
            SimEngine(SimConfig(
                seed = i.toLong(),
                playerCount = 6,
                roundCount = 3,
                forcedPool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT),
                verbosity = Verbosity.MINIMAL
            )).runGame()
        }
        println("50 buffers-only pool games passed")
    }
}
