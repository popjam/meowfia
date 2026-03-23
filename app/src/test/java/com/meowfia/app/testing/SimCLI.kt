package com.meowfia.app.testing

import com.meowfia.app.testing.analysis.BatchRunner
import com.meowfia.app.testing.reporting.BatchReportPrinter
import com.meowfia.app.testing.reporting.SweepPrinter
import com.meowfia.app.testing.reporting.VerboseGamePrinter
import com.meowfia.app.testing.sim.SimConfig
import org.junit.Test

/**
 * CLI-style simulation runner. Reads configuration from system properties.
 *
 * Usage:
 *   ./gradlew testDebugUnitTest --tests "com.meowfia.app.testing.SimCLI"
 *   ./gradlew testDebugUnitTest --tests "com.meowfia.app.testing.SimCLI" -Dsim.games=1000 -Dsim.seed=42
 *   ./gradlew testDebugUnitTest --tests "com.meowfia.app.testing.SimCLI" -Dsim.all=true
 *
 * Properties:
 *   sim.games          Number of games (default: 2000)
 *   sim.players        Number of players (default: 6)
 *   sim.rounds         Rounds per game (default: 5)
 *   sim.seed           Random seed (default: current time)
 *   sim.verbose        Print sample games (default: false)
 *   sim.samples        Number of sample games (default: 3)
 *   sim.sweep-players  Run player count sweep 4-8 (default: false)
 *   sim.sweep-rounds   Run round count sweep 3-8 (default: false)
 *   sim.all            Run everything (default: false)
 */
class SimCLI : SimTestBase() {

    @Test
    fun run() {
        val games = prop("sim.games", 2000)
        val players = prop("sim.players", 6)
        val rounds = prop("sim.rounds", 5)
        val seed = System.getProperty("sim.seed")?.toLongOrNull() ?: System.currentTimeMillis()
        val all = propBool("sim.all")
        val verbose = all || propBool("sim.verbose")
        val samples = prop("sim.samples", 3)
        val sweepPlayers = all || propBool("sim.sweep-players")
        val sweepRounds = all || propBool("sim.sweep-rounds")

        println()
        println("Meowfia Simulation CLI")
        println("  Games: $games | Players: $players | Rounds: $rounds | Seed: $seed")
        if (verbose) println("  Verbose: $samples sample games")
        if (sweepPlayers) println("  Player sweep: 4-8")
        if (sweepRounds) println("  Round sweep: 3-8")
        println()

        // Main batch
        val nVerbose = if (verbose) samples else 0
        println("Running main simulation...")
        val start = System.currentTimeMillis()
        val stats = BatchRunner.run(
            gameCount = games,
            baseConfig = SimConfig(
                playerCount = players,
                roundCount = rounds,
                seed = seed
            ),
            collectSamples = nVerbose
        )
        val elapsed = (System.currentTimeMillis() - start) / 1000.0
        println("Completed in ${"%.1f".format(elapsed)}s")

        BatchReportPrinter.print(stats)

        // Verbose sample games
        if (verbose && stats.sampleLogs.isNotEmpty()) {
            for ((i, sample) in stats.sampleLogs.withIndex()) {
                VerboseGamePrinter.print(sample, gameNum = i + 1)
            }
        }

        // Player count sweep
        if (sweepPlayers) {
            val sweepGames = maxOf(500, games / 3)
            println("Running player count sweep ($sweepGames games each)...")
            val sweepStart = System.currentTimeMillis()
            val results = mutableMapOf<Int, com.meowfia.app.testing.analysis.BatchStatistics>()
            for (np in 4..8) {
                results[np] = BatchRunner.run(
                    gameCount = sweepGames,
                    baseConfig = SimConfig(playerCount = np, roundCount = rounds, seed = seed)
                )
            }
            val sweepElapsed = (System.currentTimeMillis() - sweepStart) / 1000.0
            println("Completed in ${"%.1f".format(sweepElapsed)}s")
            SweepPrinter.printPlayerSweep(results, sweepGames, rounds)
        }

        // Round count sweep
        if (sweepRounds) {
            val sweepGames = maxOf(500, games / 3)
            println("Running round count sweep ($sweepGames games each)...")
            val sweepStart = System.currentTimeMillis()
            val results = mutableMapOf<Int, com.meowfia.app.testing.analysis.BatchStatistics>()
            for (nr in 3..8) {
                results[nr] = BatchRunner.run(
                    gameCount = sweepGames,
                    baseConfig = SimConfig(playerCount = players, roundCount = nr, seed = seed)
                )
            }
            val sweepElapsed = (System.currentTimeMillis() - sweepStart) / 1000.0
            println("Completed in ${"%.1f".format(sweepElapsed)}s")
            SweepPrinter.printRoundSweep(results, sweepGames, players)
        }

        println("Done.")
    }

    private fun prop(key: String, default: Int): Int =
        System.getProperty(key)?.toIntOrNull() ?: default

    private fun propBool(key: String): Boolean =
        System.getProperty(key)?.lowercase() in listOf("true", "1", "yes")
}
