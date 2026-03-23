package com.meowfia.app.testing.reporting

import com.meowfia.app.testing.sim.SimGameResult

/** Prints a single game play-by-play. */
object VerboseGamePrinter {

    fun print(result: SimGameResult, gameNum: Int = 1) {
        println()
        println("${"=".repeat(60)}")
        println("  SAMPLE GAME #$gameNum — Seed: ${result.seed}")
        println("${"=".repeat(60)}")
        println(result.fullLog)
        println("  FINAL SCORES: ${result.finalScores.mapIndexed { i, s -> "${result.strategies[i]}: $s" }.joinToString(", ")}")
        println()
    }
}
