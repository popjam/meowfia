package com.meowfia.app.testing.reporting

import com.meowfia.app.testing.analysis.BatchStatistics

/** Compact table output for sweep results. */
object SweepPrinter {

    fun printPlayerSweep(results: Map<Int, BatchStatistics>, gamesPerConfig: Int, roundsPerGame: Int) {
        println()
        println("  ── PLAYER COUNT SWEEP ──")
        println("  $gamesPerConfig games per configuration, $roundsPerGame rounds each")
        println()
        println("  ${"Players".padEnd(10)} ${"Avg".padStart(7)} ${"Gini".padStart(7)} ${"Skl+".padStart(7)} ${"A-C".padStart(7)} ${"Cat".padStart(7)} ${"Ded".padStart(7)} ${"Farm%".padStart(7)} ${"Via".padStart(7)}")
        println("  ${"─".repeat(75)}")

        for ((nPlayers, stats) in results.toSortedMap()) {
            println("  ${nPlayers.toString().padEnd(10)} ${
                "%.1f".format(stats.avgScore).padStart(7)
            } ${"%.3f".format(stats.avgGini).padStart(7)
            } ${"%+.1f".format(stats.skillPremium).padStart(7)
            } ${"%+.1f".format(stats.aggroConsGap).padStart(7)
            } ${"%.3f".format(stats.catchUpRate).padStart(7)
            } ${"%.3f".format(stats.deductionCorrelation).padStart(7)
            } ${"%.0f".format(stats.farmWinRate * 100).padStart(7)
            } ${"%.0f".format(stats.strategyViability * 100).padStart(6)}%")
        }
        println()
    }

    fun printRoundSweep(results: Map<Int, BatchStatistics>, gamesPerConfig: Int, nPlayers: Int) {
        println()
        println("  ── ROUND COUNT SWEEP ──")
        println("  $gamesPerConfig games per configuration, $nPlayers players each")
        println()
        println("  ${"Rounds".padEnd(10)} ${"Avg".padStart(7)} ${"Gini".padStart(7)} ${"Skl+".padStart(7)} ${"A-C".padStart(7)} ${"Cat".padStart(7)} ${"Ded".padStart(7)} ${"Farm%".padStart(7)} ${"Via".padStart(7)}")
        println("  ${"─".repeat(75)}")

        for ((nRounds, stats) in results.toSortedMap()) {
            println("  ${nRounds.toString().padEnd(10)} ${
                "%.1f".format(stats.avgScore).padStart(7)
            } ${"%.3f".format(stats.avgGini).padStart(7)
            } ${"%+.1f".format(stats.skillPremium).padStart(7)
            } ${"%+.1f".format(stats.aggroConsGap).padStart(7)
            } ${"%.3f".format(stats.catchUpRate).padStart(7)
            } ${"%.3f".format(stats.deductionCorrelation).padStart(7)
            } ${"%.0f".format(stats.farmWinRate * 100).padStart(7)
            } ${"%.0f".format(stats.strategyViability * 100).padStart(6)}%")
        }
        println()
    }
}
