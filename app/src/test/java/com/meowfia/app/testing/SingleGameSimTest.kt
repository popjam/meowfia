package com.meowfia.app.testing

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimEngine
import com.meowfia.app.testing.sim.Verbosity
import org.junit.Test

class SingleGameSimTest : SimTestBase() {

    @Test
    fun run_single_game_verbose() {
        val result = SimEngine(SimConfig(
            seed = 42,
            playerCount = 6,
            roundCount = 3,
            verbosity = Verbosity.FULL
        )).runGame()

        println(result.fullLog)
        println("\nFINAL SCORES: ${result.finalScores}")
    }

    @Test
    fun run_forced_scenario_hawk_finds_cat() {
        val result = SimEngine(SimConfig(
            seed = 100,
            playerCount = 4,
            roundCount = 1,
            forcedPool = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT, RoleId.HAWK),
            forcedAlignments = mapOf(0 to Alignment.FARM, 1 to Alignment.MEOWFIA, 2 to Alignment.FARM, 3 to Alignment.FARM),
            forcedRoles = mapOf(0 to RoleId.HAWK, 1 to RoleId.HOUSE_CAT, 2 to RoleId.PIGEON, 3 to RoleId.PIGEON),
            forcedVisits = mapOf(0 to 1, 1 to 0, 2 to 3, 3 to 2),
            verbosity = Verbosity.DEBUG
        )).runGame()

        println(result.fullLog)
        val hawkDawn = result.roundLogs[0].dawnReports.find { it.playerId == 0 }!!
        assert(hawkDawn.actualNestEggs >= 1) { "Hawk should have gained at least 1 egg from finding Meowfia" }
    }

    @Test
    fun run_zero_meowfia_round() {
        val result = SimEngine(SimConfig(
            seed = 99,
            playerCount = 6,
            roundCount = 1,
            forcedAlignments = (0..5).associateWith { Alignment.FARM },
            verbosity = Verbosity.FULL
        )).runGame()

        println(result.fullLog)
    }

    @Test
    fun run_all_meowfia_round() {
        val result = SimEngine(SimConfig(
            seed = 77,
            playerCount = 6,
            roundCount = 1,
            forcedAlignments = (0..5).associateWith { Alignment.MEOWFIA },
            verbosity = Verbosity.FULL
        )).runGame()

        println(result.fullLog)
    }
}
