package com.meowfia.app.testing

import com.meowfia.app.testing.analysis.BatchRunner
import com.meowfia.app.testing.reporting.SweepPrinter
import com.meowfia.app.testing.sim.SimConfig
import org.junit.Test

class RoundCountSweepTest : SimTestBase() {

    @Test
    fun sweep_round_counts_3_to_8() {
        val gamesPerConfig = 1000
        val nPlayers = 6
        val results = mutableMapOf<Int, com.meowfia.app.testing.analysis.BatchStatistics>()

        for (nRounds in 3..8) {
            results[nRounds] = BatchRunner.run(
                gameCount = gamesPerConfig,
                baseConfig = SimConfig(playerCount = nPlayers, roundCount = nRounds, seed = 1)
            )
        }

        SweepPrinter.printRoundSweep(results, gamesPerConfig, nPlayers)
    }
}
