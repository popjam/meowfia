package com.meowfia.app.testing.sim

import com.meowfia.app.bot.BotStrategy
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.util.RandomProvider

/**
 * Controls a simulated player's decisions.
 *
 * Night targeting is handled by [com.meowfia.app.bot.BotBrain] via [toBotStrategy].
 * This class handles voting and card-throwing (sim-only concerns).
 *
 * @param nightSkill  0.0–1.0 — How well this player identifies correct targets at night.
 * @param deduction   0.0–1.0 — Probability of correctly identifying Meowfia during voting.
 * @param aggression  0.0–1.0 — How many cards thrown per vote.
 */
data class SimStrategy(
    val name: String,
    val nightSkill: Float,
    val deduction: Float,
    val aggression: Float
) {
    /** Convert to a [BotStrategy] for use with BotBrain night targeting. */
    fun toBotStrategy(): BotStrategy = BotStrategy(name = name, nightSkill = nightSkill)

    /** Choose who to throw eggs at during voting. */
    fun chooseVoteTarget(
        actor: SimPlayer,
        allPlayers: List<SimPlayer>,
        random: RandomProvider
    ): Int {
        val others = allPlayers.filter { it.id != actor.id }

        // Deduction: probability of correctly targeting the opposite team
        val isCorrect = random.nextFloat() < deduction

        val candidates = if (isCorrect) {
            val opposites = others.filter { it.alignment != actor.alignment }
            if (opposites.isNotEmpty()) opposites else others
        } else {
            others
        }

        return candidates[random.nextInt(candidates.size)].id
    }

    /** Choose which cards to throw and which to keep. v6: purely value-based. */
    fun chooseThrow(
        hand: List<SimCard>,
        confidence: Float,
        random: RandomProvider
    ): ThrowDecision {
        if (hand.isEmpty()) return ThrowDecision(emptyList(), emptyList())

        val throwCount = maxOf(1, (hand.size * aggression).toInt())

        // v6: sort by value — wilds first (0 risk), then low-value, then high-value
        // Higher confidence makes the player willing to throw higher-value cards
        val sorted = hand.sortedBy { card ->
            if (card.suit == Suit.WILD) -100f
            else card.value * (1f - confidence * 0.5f)
        }

        return ThrowDecision(
            thrown = sorted.take(throwCount),
            kept = sorted.drop(throwCount)
        )
    }

    /** Confidence estimate for this throw. */
    fun getConfidence(isTargetingCorrectTeam: Boolean, random: RandomProvider): Float {
        val baseConfidence = if (isTargetingCorrectTeam) 0.7f else 0.3f
        val noise = (random.nextFloat() - 0.5f) * 0.3f
        return (baseConfidence + noise * (1f - deduction)).coerceIn(0.1f, 0.95f)
    }
}

data class ThrowDecision(
    val thrown: List<SimCard>,
    val kept: List<SimCard>
)
