package com.meowfia.app.testing.reporting

import com.meowfia.app.testing.analysis.BatchStatistics

/** Prints the full analysis report to stdout, mirroring the Python harness output format. */
object BatchReportPrinter {

    fun print(stats: BatchStatistics) {
        println()
        println("${"=".repeat(65)}")
        println("  MEOWFIA SIMULATION ANALYSIS — ${stats.nGames} Games")
        println("  Config: ${stats.nPlayers} players × ${stats.nRounds} rounds per game")
        println("${"=".repeat(65)}")

        // Score distribution
        println()
        println("  ── SCORE DISTRIBUTION ──")
        println("    Average:   ${"%.1f".format(stats.avgScore)}")
        println("    Median:    ${"%.0f".format(stats.medianScore)}")
        println("    Min:       ${stats.minScore}")
        println("    Max:       ${stats.maxScore}")
        println("    Std Dev:   ${"%.1f".format(stats.scoreStdDev)}")
        println("    Negative:  ${"%.1f".format(stats.negativeScorePct)}%")

        // Balance metrics
        println()
        println("  ── BALANCE METRICS ──")
        println("    Score Equality (Gini)        ${"%.3f".format(stats.avgGini)}   (0=equal, 1=unequal. Target: <0.35)")
        println("    Skill Premium                ${"%+.1f".format(stats.skillPremium)}   (Skilled vs unskilled. Target: +15 to +25)")
        println("    Aggro-Conservative Gap       ${"%+.1f".format(stats.aggroConsGap)}   (Target: -5 to +5)")
        println("    Catch-up Rate                ${"%.3f".format(stats.catchUpRate)}   (Rank mobility mid->end. Target: >0.35)")
        println("    Deduction Correlation        ${"%.3f".format(stats.deductionCorrelation)}   (Correct throws -> score. Target: >0.55)")
        println("    Strategy Viability           ${"%.0f".format(stats.strategyViability * 100)}%   (Target: >66%)")
        println("    Strategy Spread              ${"%.1f".format(stats.strategySpread)}   (Lower = more balanced)")

        // Strategy performance
        println()
        println("  ── STRATEGY PERFORMANCE ──")
        println("    ${"Archetype".padEnd(25)} ${"Avg Score".padStart(10)} ${"Std Dev".padStart(10)} ${"vs Best".padStart(10)}")
        println("    ${"─".repeat(55)}")
        val bestMean = stats.strategyMeans.values.maxOrNull() ?: 0.0
        for ((name, mean) in stats.strategyMeans.entries.sortedByDescending { it.value }) {
            val std = stats.strategyStdDevs[name] ?: 0.0
            val diff = mean - bestMean
            val star = if (diff == 0.0) " ★" else ""
            println("    ${name.padEnd(25)} ${"%.1f".format(mean).padStart(10)} ${"%.1f".format(std).padStart(10)} ${"%+.1f".format(diff).padStart(10)}$star")
        }

        // Night resolution metrics
        println()
        println("  ── NIGHT RESOLUTION METRICS ──")
        println("    Avg eggs created/round:   ${"%.1f".format(stats.nightMetrics.avgEggsCreatedPerRound)}")
        println("    Zero egg delta rate:      ${"%.1f".format(stats.nightMetrics.zeroEggDeltaRate * 100)}%")
        println("    Avg info lines/player:    ${"%.1f".format(stats.nightMetrics.avgInfoLinesPerPlayer)}")

        // Role performance
        if (stats.roleMetrics.isNotEmpty()) {
            println()
            println("  ── ROLE PERFORMANCE ──")
            println("    ${"Role".padEnd(16)} ${"Assigned".padStart(10)}")
            println("    ${"─".repeat(30)}")
            for ((roleId, roleStats) in stats.roleMetrics.entries.sortedByDescending { it.value.timesAssigned }) {
                println("    ${roleId.displayName.padEnd(16)} ${roleStats.timesAssigned.toString().padStart(10)}")
            }
        }

        // Alignment
        println()
        println("  ── ALIGNMENT ──")
        println("    Farm win rate:         ${"%.1f".format(stats.farmWinRate * 100)}%")
        println("    Zero-Meowfia rounds:   ${"%.1f".format(stats.zeroMeowfiaRate * 100)}%")
        println("    All-Meowfia rounds:    ${"%.1f".format(stats.allMeowfiaRate * 100)}%")

        // Health check
        println()
        println("  ── HEALTH CHECK ──")
        printCheck("Score equality healthy", stats.avgGini < 0.35)
        printCheck("Skill premium in range", stats.skillPremium in 5.0..30.0)
        printCheck("Aggro-conservative balanced", stats.aggroConsGap in -10.0..10.0)
        printCheck("Catch-up potential adequate", stats.catchUpRate >= 0.3)
        printCheck("Deduction rewarded", stats.deductionCorrelation >= 0.5)
        printCheck("Multiple strategies viable", stats.strategyViability >= 0.66)
        printCheck("Farm win rate reasonable", stats.farmWinRate in 0.35..0.65)

        println()
    }

    fun printCompact(stats: BatchStatistics, label: String) {
        println("  $label: avg=${"%.1f".format(stats.avgScore)} gini=${"%.3f".format(stats.avgGini)} skill+=${"%.1f".format(stats.skillPremium)} cat=${"%.3f".format(stats.catchUpRate)} ded=${"%.3f".format(stats.deductionCorrelation)} farm=${"%.0f".format(stats.farmWinRate * 100)}%")
    }

    private fun printCheck(label: String, ok: Boolean) {
        val icon = if (ok) "✓" else "✗"
        println("    $icon $label")
    }
}
