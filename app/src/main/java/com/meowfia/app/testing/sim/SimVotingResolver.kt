package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.util.RandomProvider

/**
 * Simulates the physical voting phase and scoring resolution.
 * Uses v6 rules: no suit effects, uniform vote weight, consolation returns to hand.
 */
class SimVotingResolver {

    data class VotingResult(
        val targets: Map<Int, Int>,
        val thrown: Map<Int, List<SimCard>>,
        val kept: Map<Int, List<SimCard>>,
        val votes: Map<Int, Int>,
        val eliminatedId: Int,
        val eliminatedAlignment: Alignment,
        val winningTeam: Alignment
    )

    data class ScoringEvent(
        val playerId: Int,
        val description: String
    )

    fun resolve(
        simPlayers: List<SimPlayer>,
        assignments: List<PlayerAssignment>,
        dawnReports: List<DawnReport>,
        random: RandomProvider
    ): VotingResult {
        val targets = mutableMapOf<Int, Int>()
        val thrown = mutableMapOf<Int, List<SimCard>>()
        val kept = mutableMapOf<Int, List<SimCard>>()
        val votes = mutableMapOf<Int, Int>()

        for (sp in simPlayers) {
            val targetId = sp.strategy.chooseVoteTarget(sp, simPlayers, random)
            targets[sp.id] = targetId

            val targetAlignment = assignments.find { it.playerId == targetId }?.alignment ?: Alignment.FARM
            val isCorrect = sp.alignment != targetAlignment
            val confidence = sp.strategy.getConfidence(isCorrect, random)
            val decision = sp.strategy.chooseThrow(sp.hand.toList(), confidence, random)

            thrown[sp.id] = decision.thrown
            kept[sp.id] = decision.kept

            // v6: every thrown card = 1 vote, no exceptions
            val voteWeight = decision.thrown.size
            votes[targetId] = (votes[targetId] ?: 0) + voteWeight
        }

        // Determine eliminated — most votes, ties favor Meowfia getting eliminated
        val maxVotes = votes.values.maxOrNull() ?: 0
        val tiedPlayers = votes.filter { it.value == maxVotes }.keys
        val eliminatedId = if (tiedPlayers.size == 1) {
            tiedPlayers.first()
        } else {
            // Ties: prefer eliminating Farm (Meowfia wins ties)
            val farmTied = tiedPlayers.filter { pid ->
                assignments.find { it.playerId == pid }?.alignment == Alignment.FARM
            }
            if (farmTied.isNotEmpty()) farmTied.first() else tiedPlayers.first()
        }

        val eliminatedAlignment = assignments.find { it.playerId == eliminatedId }?.alignment ?: Alignment.FARM
        val winningTeam = if (eliminatedAlignment == Alignment.MEOWFIA) Alignment.FARM else Alignment.MEOWFIA

        // Remove thrown cards from hands
        for (sp in simPlayers) {
            val thrownCards = thrown[sp.id] ?: continue
            for (card in thrownCards) {
                sp.hand.remove(card)
            }
        }

        return VotingResult(targets, thrown, kept, votes, eliminatedId, eliminatedAlignment, winningTeam)
    }

    fun resolveScoring(
        simPlayers: List<SimPlayer>,
        votingResult: VotingResult,
        assignments: List<PlayerAssignment>,
        random: RandomProvider
    ): List<ScoringEvent> {
        val events = mutableListOf<ScoringEvent>()

        for (sp in simPlayers) {
            val cards = votingResult.thrown[sp.id] ?: continue
            val isWinner = sp.alignment == votingResult.winningTeam
            val targetId = votingResult.targets[sp.id] ?: continue

            if (isWinner) {
                // v6: bank all thrown cards face-down to score pile
                for (card in cards) {
                    sp.scorePile.add(card)
                }
                events.add(ScoringEvent(sp.id, "${sp.name} banks ${cards.size} cards to score pile"))
            } else {
                val targetAlignment = assignments.find { it.playerId == targetId }?.alignment
                val targetedOpposite = sp.alignment != targetAlignment
                val remaining = cards.toMutableList()

                // v6: correct-target consolation returns best card to HAND
                if (targetedOpposite && remaining.isNotEmpty()) {
                    remaining.sortByDescending { it.value }
                    val bestCard = remaining.removeAt(0)
                    sp.hand.add(bestCard)
                    events.add(ScoringEvent(sp.id, "${sp.name} targeted opposite team, returns ${bestCard.display} to hand"))
                }

                // All remaining cards → discard (no suit penalties)
                if (remaining.isNotEmpty()) {
                    events.add(ScoringEvent(sp.id, "${sp.name} discards ${remaining.size} cards"))
                }
            }
        }

        return events
    }
}
