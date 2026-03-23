package com.meowfia.app.testing.reporting

import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.testing.sim.RoundSolver
import com.meowfia.app.testing.sim.SimPlayer
import com.meowfia.app.testing.sim.SimVotingResolver
import com.meowfia.app.testing.sim.Verbosity

/** Structured text output builder for simulation logs. */
class SimLogger(private val verbosity: Verbosity) {
    private val buffer = StringBuilder()

    fun header(title: String) {
        if (verbosity == Verbosity.MINIMAL) return
        buffer.appendLine("${"=".repeat(60)}")
        buffer.appendLine("  $title")
        buffer.appendLine("${"=".repeat(60)}")
    }

    fun line(text: String) {
        if (verbosity == Verbosity.MINIMAL) return
        buffer.appendLine("  $text")
    }

    fun separator() {
        if (verbosity == Verbosity.MINIMAL) return
        buffer.appendLine("${"─".repeat(60)}")
    }

    fun roundHeader(roundNum: Int) {
        if (verbosity == Verbosity.MINIMAL) return
        buffer.appendLine()
        buffer.appendLine("${"─".repeat(4)} ROUND $roundNum ${"─".repeat(48)}")
    }

    fun poolReveal(pool: List<PoolCard>, flowers: List<RoleId>) {
        if (verbosity.ordinal < Verbosity.SUMMARY.ordinal) return
        buffer.appendLine("  Pool: ${pool.joinToString { it.roleId.displayName }}")
        if (flowers.isNotEmpty()) {
            buffer.appendLine("  Active flowers: ${flowers.joinToString { it.displayName }}")
        }
    }

    fun assignments(assignments: List<PlayerAssignment>, dealerSeat: Int) {
        if (verbosity.ordinal < Verbosity.FULL.ordinal) return
        buffer.appendLine("  Dealer: seat $dealerSeat")
        for (a in assignments) {
            buffer.appendLine("    [${a.playerId}] ${a.alignment.displayName} — ${a.roleId.displayName}")
        }
    }

    fun nightActions(players: List<SimPlayer>, state: GameState) {
        if (verbosity.ordinal < Verbosity.DEBUG.ordinal) return
        buffer.appendLine("  Night actions:")
        for (p in players) {
            val target = state.visitGraph[p.id]
            buffer.appendLine("    ${p.name} (${p.roleId.displayName}) → ${target ?: "auto/self"}")
        }
    }

    fun nightResolution(state: GameState) {
        if (verbosity.ordinal < Verbosity.FULL.ordinal) return
        buffer.appendLine("  RESOLUTION LOG:")
        for (result in state.nightResults.values) {
            if (result.narrative.isNotBlank()) {
                for (line in result.narrative.lines().take(1)) {
                    buffer.appendLine("    $line")
                }
            }
        }
    }

    fun dawnReports(reports: List<DawnReport>, players: List<SimPlayer>) {
        if (verbosity.ordinal < Verbosity.FULL.ordinal) return
        buffer.appendLine("  Dawn reports:")
        for (report in reports) {
            val name = players.find { it.id == report.playerId }?.name ?: "?"
            val delta = report.reportedEggDelta
        val deltaStr = if (delta >= 0) "+$delta" else "$delta"
        buffer.appendLine("    $name: ${deltaStr} eggs${if (report.isConfused) " (confused)" else ""}")
        }
    }

    fun votingResult(result: SimVotingResolver.VotingResult, players: List<SimPlayer>, assignments: List<PlayerAssignment>) {
        if (verbosity.ordinal < Verbosity.SUMMARY.ordinal) return
        val eliminated = players.find { it.id == result.eliminatedId }
        buffer.appendLine("  Eliminated: ${eliminated?.name} (${result.eliminatedAlignment.displayName})")
        buffer.appendLine("  Winner: ${result.winningTeam.displayName}")
    }

    fun scoringEvents(events: List<SimVotingResolver.ScoringEvent>) {
        if (verbosity.ordinal < Verbosity.FULL.ordinal) return
        if (events.isEmpty()) return
        buffer.appendLine("  Scoring:")
        for (event in events) {
            buffer.appendLine("    ${event.description}")
        }
    }

    fun solvability(result: RoundSolver.SolvabilityResult) {
        if (verbosity.ordinal < Verbosity.SUMMARY.ordinal) return
        val tag = when (result.solvability) {
            RoundSolver.Solvability.SOLVED -> "SOLVED"
            RoundSolver.Solvability.NARROWED -> "NARROWED"
            RoundSolver.Solvability.COIN_FLIP -> "COIN FLIP"
        }
        buffer.appendLine("  Deducibility: $tag (${result.consistentWorlds}/${result.totalCandidates} consistent worlds)")
        if (result.cleared.isNotEmpty()) {
            buffer.appendLine("    Cleared: ${result.cleared.joinToString()}")
        }
        if (result.suspects.isNotEmpty() && result.solvability != RoundSolver.Solvability.COIN_FLIP) {
            buffer.appendLine("    Suspects: ${result.suspects.joinToString()}")
        }
        if (verbosity.ordinal >= Verbosity.FULL.ordinal) {
            for (reason in result.reasons) {
                buffer.appendLine("    - $reason")
            }
        }
    }

    fun getFullLog(): String = buffer.toString()
}
