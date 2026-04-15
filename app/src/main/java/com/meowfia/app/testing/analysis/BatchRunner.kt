package com.meowfia.app.testing.analysis

import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimEngine
import com.meowfia.app.testing.sim.SimGameResult
import com.meowfia.app.testing.sim.Verbosity

/** Runs N games and aggregates results into [BatchStatistics]. */
object BatchRunner {

    fun run(
        gameCount: Int,
        baseConfig: SimConfig,
        collectSamples: Int = 0,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): BatchStatistics {
        val allResults = mutableListOf<SimGameResult>()
        val sampleLogs = mutableListOf<SimGameResult>()

        for (i in 0 until gameCount) {
            val config = baseConfig.copy(
                seed = (baseConfig.seed ?: System.currentTimeMillis()) + i,
                verbosity = if (i < collectSamples) Verbosity.FULL else Verbosity.MINIMAL
            )
            val result = SimEngine(config).runGame()
            allResults.add(result)
            if (i < collectSamples) sampleLogs.add(result)
            onProgress?.invoke(i + 1, gameCount)
        }

        return BalanceMetrics.analyze(allResults, baseConfig, sampleLogs)
    }
}
