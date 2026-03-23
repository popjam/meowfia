package com.meowfia.app.testing

import com.meowfia.app.testing.analysis.BatchRunner
import com.meowfia.app.testing.reporting.BatchReportPrinter
import com.meowfia.app.testing.reporting.VerboseGamePrinter
import com.meowfia.app.testing.sim.SimConfig
import org.junit.Test

class BatchSimTest : SimTestBase() {

    @Test
    fun run_quick_batch_500_games() {
        val stats = BatchRunner.run(
            gameCount = 500,
            baseConfig = SimConfig(playerCount = 6, roundCount = 5, seed = 1)
        )
        BatchReportPrinter.print(stats)
    }

    @Test
    fun run_standard_batch_2000_games() {
        val stats = BatchRunner.run(
            gameCount = 2000,
            baseConfig = SimConfig(playerCount = 6, roundCount = 5, seed = 1),
            collectSamples = 3
        )
        BatchReportPrinter.print(stats)
        for ((i, sample) in stats.sampleLogs.withIndex()) {
            VerboseGamePrinter.print(sample, gameNum = i + 1)
        }
    }
}
