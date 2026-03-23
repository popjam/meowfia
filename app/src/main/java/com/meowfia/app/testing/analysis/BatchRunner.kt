package com.meowfia.app.testing.analysis

import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimEngine
import com.meowfia.app.testing.sim.Verbosity

/** Runs N games and aggregates results into [BatchStatistics]. */
object BatchRunner {

    fun run(
        gameCount: Int,
        baseConfig: SimConfig,
        collectSamples: Int = 0
    ): BatchStatistics {
        val allResults = mutableListOf<com.meowfia.app.testing.sim.SimGameResult>()
        val sampleLogs = mutableListOf<com.meowfia.app.testing.sim.SimGameResult>()

        for (i in 0 until gameCount) {
            val config = baseConfig.copy(
                seed = (baseConfig.seed ?: System.currentTimeMillis()) + i,
                verbosity = if (i < collectSamples) Verbosity.FULL else Verbosity.MINIMAL
            )
            val result = SimEngine(config).runGame()
            allResults.add(result)
            if (i < collectSamples) sampleLogs.add(result)
        }

        return BalanceMetrics.analyze(allResults, baseConfig, sampleLogs)
    }
}
