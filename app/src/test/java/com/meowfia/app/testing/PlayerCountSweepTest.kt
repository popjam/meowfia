package com.meowfia.app.testing

import com.meowfia.app.testing.analysis.BatchRunner
import com.meowfia.app.testing.reporting.SweepPrinter
import com.meowfia.app.testing.sim.SimConfig
import org.junit.Test

class PlayerCountSweepTest : SimTestBase() {

    @Test
    fun sweep_player_counts_4_to_8() {
        val gamesPerConfig = 1000
        val roundsPerGame = 5
        val results = mutableMapOf<Int, com.meowfia.app.testing.analysis.BatchStatistics>()

        for (nPlayers in 4..8) {
            results[nPlayers] = BatchRunner.run(
                gameCount = gamesPerConfig,
                baseConfig = SimConfig(playerCount = nPlayers, roundCount = roundsPerGame, seed = 1)
            )
        }

        SweepPrinter.printPlayerSweep(results, gamesPerConfig, roundsPerGame)
    }
}
