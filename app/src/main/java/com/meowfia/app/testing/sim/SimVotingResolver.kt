package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.util.RandomProvider

/**
 * Simulates the physical voting phase and scoring resolution.
 * Uses v5 suit rules exactly.
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
        val description: String,
        val suit: Suit? = null,
        val isWin: Boolean = false,
        val cardValue: Int = 0
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

            // Count votes (clubs = 2)
            val voteWeight = decision.thrown.sumOf { card: SimCard ->
                if (card.suit == Suit.CLUBS) 2.toInt() else 1.toInt()
            }
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
            val targetPlayer = simPlayers.find { it.id == targetId } ?: continue

            if (isWinner) {
                for (card in cards) {
                    sp.scorePile.add(card)
                    events.add(ScoringEvent(sp.id, "${sp.name} banks ${card.display}", card.suit, true, card.value))

                    when (card.suit) {
                        Suit.DIAMONDS -> {
                            sp.lockBestHandToScore()
                            events.add(ScoringEvent(sp.id, "  Diamond locked best hand card into score pile"))
                        }
                        Suit.SPADES -> {
                            if (targetPlayer.hand.isNotEmpty()) {
                                val stolen = targetPlayer.hand.removeAt(random.nextInt(targetPlayer.hand.size))
                                sp.hand.add(stolen)
                                events.add(ScoringEvent(sp.id, "  Spade stole ${stolen.display} from ${targetPlayer.name}"))
                            }
                        }
                        else -> {}
                    }
                }
            } else {
                val targetAlignment = assignments.find { it.playerId == targetId }?.alignment
                val targetedOpposite = sp.alignment != targetAlignment
                val remaining = cards.toMutableList()

                if (targetedOpposite && remaining.isNotEmpty()) {
                    remaining.sortByDescending { it.value }
                    val banked = remaining.removeFirst()
                    sp.scorePile.add(banked)
                    events.add(ScoringEvent(sp.id, "${sp.name} targeted opposite team, banks ${banked.display}"))
                }

                for (card in remaining) {
                    when (card.suit) {
                        Suit.DIAMONDS -> {
                            sp.moveBestScoreToHand()
                            events.add(ScoringEvent(sp.id, "  Diamond lost ${card.display}, demoted score card", card.suit, false, card.value))
                        }
                        Suit.CLUBS -> {
                            targetPlayer.scorePile.add(card)
                            events.add(ScoringEvent(sp.id, "  Club lost ${card.display}, gifted to ${targetPlayer.name}", card.suit, false, card.value))
                        }
                        Suit.SPADES -> {
                            if (sp.hand.isNotEmpty()) {
                                val given = sp.hand.removeAt(random.nextInt(sp.hand.size))
                                targetPlayer.hand.add(given)
                                events.add(ScoringEvent(sp.id, "  Spade lost ${card.display}, gave ${given.display} to ${targetPlayer.name}", card.suit, false, card.value))
                            }
                        }
                        else -> {
                            events.add(ScoringEvent(sp.id, "  ${card.display} discarded (no penalty)", card.suit, false, card.value))
                        }
                    }
                }
            }
        }

        return events
    }
}
