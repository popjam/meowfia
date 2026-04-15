package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.util.RandomProvider

/**
 * Simulates the physical voting phase and scoring resolution.
 * Configurable via [ScoringRules]; defaults match v6 Meowfia rules.
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
        random: RandomProvider,
        roundNum: Int = 1,
        totalRounds: Int = 1,
        scoringRules: ScoringRules = ScoringRules()
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
            val decision = sp.strategy.chooseThrow(sp.hand.toList(), confidence, random, roundNum, totalRounds)

            // Enforce minimum throw per round
            val thrownCards = if (decision.thrown.size < scoringRules.minThrowPerRound && sp.hand.size >= scoringRules.minThrowPerRound) {
                // Force throw minimum by taking from kept
                val allCards = (decision.thrown + decision.kept).toMutableList()
                allCards.sortByDescending { it.value }
                val forcedThrown = allCards.take(scoringRules.minThrowPerRound)
                forcedThrown
            } else {
                decision.thrown
            }
            val keptCards = if (thrownCards !== decision.thrown) {
                (decision.thrown + decision.kept).toMutableList().also { it.removeAll(thrownCards.toSet()) }
            } else {
                decision.kept
            }

            thrown[sp.id] = thrownCards
            kept[sp.id] = keptCards

            // Every thrown card = 1 vote
            val voteWeight = thrownCards.size
            votes[targetId] = (votes[targetId] ?: 0) + voteWeight
        }

        // Determine eliminated — most votes, tie resolution based on scoringRules.tieBreaker
        val maxVotes = votes.values.maxOrNull() ?: 0
        val tiedPlayers = votes.filter { it.value == maxVotes }.keys
        val eliminatedId = if (tiedPlayers.size == 1) {
            tiedPlayers.first()
        } else {
            resolveTie(tiedPlayers, assignments, simPlayers, random, scoringRules.tieBreaker)
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

    private fun resolveTie(
        tiedPlayers: Set<Int>,
        assignments: List<PlayerAssignment>,
        simPlayers: List<SimPlayer>,
        random: RandomProvider,
        tieBreaker: TieBreaker
    ): Int {
        return when (tieBreaker) {
            TieBreaker.MEOWFIA_WINS -> {
                // Prefer eliminating Farm (Meowfia wins ties)
                val farmTied = tiedPlayers.filter { pid ->
                    assignments.find { it.playerId == pid }?.alignment == Alignment.FARM
                }
                if (farmTied.isNotEmpty()) farmTied.first() else tiedPlayers.first()
            }
            TieBreaker.FARM_WINS -> {
                // Prefer eliminating Meowfia (Farm wins ties)
                val meowfiaTied = tiedPlayers.filter { pid ->
                    assignments.find { it.playerId == pid }?.alignment == Alignment.MEOWFIA
                }
                if (meowfiaTied.isNotEmpty()) meowfiaTied.first() else tiedPlayers.first()
            }
            TieBreaker.RANDOM -> {
                val list = tiedPlayers.toList()
                list[random.nextInt(list.size)]
            }
            TieBreaker.HIGHEST_CARD_VALUE, TieBreaker.LOWEST_CARD_VALUE -> {
                // Fall back to random for card-value ties (not enough context here)
                val list = tiedPlayers.toList()
                list[random.nextInt(list.size)]
            }
        }
    }

    fun resolveScoring(
        simPlayers: List<SimPlayer>,
        votingResult: VotingResult,
        assignments: List<PlayerAssignment>,
        random: RandomProvider,
        scoringRules: ScoringRules = ScoringRules()
    ): List<ScoringEvent> {
        val events = mutableListOf<ScoringEvent>()

        for (sp in simPlayers) {
            val cards = votingResult.thrown[sp.id] ?: continue
            val isWinner = sp.alignment == votingResult.winningTeam
            val targetId = votingResult.targets[sp.id] ?: continue
            val targetAlignment = assignments.find { it.playerId == targetId }?.alignment
            val targetedOpposite = sp.alignment != targetAlignment
            val targetPlayer = simPlayers.find { it.id == targetId }

            if (isWinner) {
                // Check wrong-target penalty (won but targeted own team)
                if (!targetedOpposite) {
                    applyWrongTargetPenalty(sp, targetPlayer, scoringRules.wrongTargetPenalty, events)
                }

                // Win thrown action
                when (scoringRules.winThrownAction) {
                    WinThrownAction.BANK_TO_SCORE_PILE -> {
                        for (card in cards) {
                            sp.scorePile.add(card)
                        }
                        events.add(ScoringEvent(sp.id, "${sp.name} banks ${cards.size} cards to score pile"))
                    }
                    WinThrownAction.RETURN_TO_HAND -> {
                        for (card in cards) {
                            sp.hand.add(card)
                        }
                        events.add(ScoringEvent(sp.id, "${sp.name} returns ${cards.size} thrown cards to hand"))
                    }
                    WinThrownAction.FLAT_POINTS_PER_CARD -> {
                        val pts = cards.size * scoringRules.flatWinPointsPerCard
                        sp.bonusPoints += pts
                        events.add(ScoringEvent(sp.id, "${sp.name} earns $pts bonus points (${cards.size} cards x ${scoringRules.flatWinPointsPerCard})"))
                    }
                }

                // Flat win bonus points
                if (scoringRules.flatWinPoints > 0) {
                    sp.bonusPoints += scoringRules.flatWinPoints
                    events.add(ScoringEvent(sp.id, "${sp.name} earns ${scoringRules.flatWinPoints} flat win bonus points"))
                }
            } else {
                // Loser — apply consolation first if targeted opposite team
                val remaining = cards.toMutableList()

                if (targetedOpposite && remaining.isNotEmpty()) {
                    applyConsolation(sp, remaining, scoringRules.consolation, events)
                }

                // Loss thrown action on remaining cards
                if (remaining.isNotEmpty()) {
                    when (scoringRules.lossThrownAction) {
                        LossThrownAction.DISCARD -> {
                            events.add(ScoringEvent(sp.id, "${sp.name} discards ${remaining.size} cards"))
                        }
                        LossThrownAction.RETURN_TO_HAND -> {
                            for (card in remaining) {
                                sp.hand.add(card)
                            }
                            events.add(ScoringEvent(sp.id, "${sp.name} returns ${remaining.size} cards to hand"))
                        }
                        LossThrownAction.GIVE_TO_TARGET_SCORE -> {
                            if (targetPlayer != null) {
                                for (card in remaining) {
                                    targetPlayer.scorePile.add(card)
                                }
                                events.add(ScoringEvent(sp.id, "${sp.name} gives ${remaining.size} cards to ${targetPlayer.name}'s score pile"))
                            } else {
                                events.add(ScoringEvent(sp.id, "${sp.name} discards ${remaining.size} cards (target not found)"))
                            }
                        }
                        LossThrownAction.NEGATIVE_SCORE -> {
                            val penalty = remaining.sumOf { it.value }
                            sp.bonusPoints -= penalty
                            events.add(ScoringEvent(sp.id, "${sp.name} takes -$penalty negative score from ${remaining.size} cards"))
                        }
                        LossThrownAction.FLAT_PENALTY_PER_CARD -> {
                            val penalty = remaining.size * scoringRules.flatLossPenaltyPoints
                            sp.bonusPoints -= penalty
                            events.add(ScoringEvent(sp.id, "${sp.name} takes -$penalty penalty (${remaining.size} cards)"))
                        }
                    }
                }
            }
        }

        return events
    }

    private fun applyConsolation(
        sp: SimPlayer,
        remaining: MutableList<SimCard>,
        consolation: Consolation,
        events: MutableList<ScoringEvent>
    ) {
        when (consolation) {
            Consolation.NONE -> {
                // No consolation
            }
            Consolation.RETURN_HIGHEST_TO_HAND -> {
                remaining.sortByDescending { it.value }
                val bestCard = remaining.removeAt(0)
                sp.hand.add(bestCard)
                events.add(ScoringEvent(sp.id, "${sp.name} targeted opposite team, returns ${bestCard.display} to hand"))
            }
            Consolation.RETURN_ALL_TO_HAND -> {
                for (card in remaining.toList()) {
                    sp.hand.add(card)
                }
                events.add(ScoringEvent(sp.id, "${sp.name} targeted opposite team, returns all ${remaining.size} cards to hand"))
                remaining.clear()
            }
            Consolation.BANK_HIGHEST_TO_SCORE -> {
                remaining.sortByDescending { it.value }
                val bestCard = remaining.removeAt(0)
                sp.scorePile.add(bestCard)
                events.add(ScoringEvent(sp.id, "${sp.name} targeted opposite team, banks ${bestCard.display} to score pile"))
            }
            Consolation.FLAT_POINTS -> {
                // Small flat consolation bonus
                sp.bonusPoints += 1
                events.add(ScoringEvent(sp.id, "${sp.name} targeted opposite team, earns 1 consolation point"))
            }
        }
    }

    private fun applyWrongTargetPenalty(
        sp: SimPlayer,
        targetPlayer: SimPlayer?,
        penalty: WrongTargetPenalty,
        events: MutableList<ScoringEvent>
    ) {
        when (penalty) {
            WrongTargetPenalty.NONE -> { /* no penalty */ }
            WrongTargetPenalty.RETURN_LOWEST, WrongTargetPenalty.DISCARD_LOWEST -> {
                if (sp.scorePile.isNotEmpty()) {
                    sp.scorePile.sortBy { it.value }
                    val lowest = sp.scorePile.removeAt(0)
                    if (penalty == WrongTargetPenalty.RETURN_LOWEST) {
                        sp.hand.add(lowest)
                        events.add(ScoringEvent(sp.id, "${sp.name} targeted own team: returns ${lowest.display} from score pile to hand"))
                    } else {
                        events.add(ScoringEvent(sp.id, "${sp.name} targeted own team: discards ${lowest.display} from score pile"))
                    }
                }
            }
            WrongTargetPenalty.FLAT_POINTS -> {
                sp.bonusPoints -= 1
                events.add(ScoringEvent(sp.id, "${sp.name} targeted own team: -1 penalty point"))
            }
            WrongTargetPenalty.GIVE_LOWEST_TO_TARGET_HAND -> {
                if (sp.scorePile.isNotEmpty() && targetPlayer != null) {
                    sp.scorePile.sortBy { it.value }
                    val lowest = sp.scorePile.removeAt(0)
                    targetPlayer.hand.add(lowest)
                    events.add(ScoringEvent(sp.id, "${sp.name} targeted own team: gives ${lowest.display} to ${targetPlayer.name}'s hand"))
                }
            }
            WrongTargetPenalty.GIVE_LOWEST_TO_TARGET_SCORE -> {
                if (sp.scorePile.isNotEmpty() && targetPlayer != null) {
                    sp.scorePile.sortBy { it.value }
                    val lowest = sp.scorePile.removeAt(0)
                    targetPlayer.scorePile.add(lowest)
                    events.add(ScoringEvent(sp.id, "${sp.name} targeted own team: gives ${lowest.display} to ${targetPlayer.name}'s score pile"))
                }
            }
        }
    }
}
