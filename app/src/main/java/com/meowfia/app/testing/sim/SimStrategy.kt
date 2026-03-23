package com.meowfia.app.testing.sim

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.util.RandomProvider

/**
 * Controls a simulated player's decisions.
 *
 * @param nightSkill  0.0–1.0 — How well this player uses night info.
 * @param deduction   0.0–1.0 — Probability of correctly identifying Meowfia during voting.
 * @param aggression  0.0–1.0 — How many cards thrown per vote.
 * @param suitSavvy   0.0–1.0 — How intelligently suits are prioritized.
 */
data class SimStrategy(
    val name: String,
    val nightSkill: Float,
    val deduction: Float,
    val aggression: Float,
    val suitSavvy: Float
) {
    /** Choose who to visit during night phase. Returns null for auto-target roles. */
    fun chooseNightTarget(
        actor: SimPlayer,
        allPlayers: List<SimPlayer>,
        random: RandomProvider
    ): Int? {
        val others = allPlayers.filter { it.id != actor.id }
        if (others.isEmpty()) return null

        return when (actor.roleId) {
            RoleId.HAWK -> {
                // Skilled hawk more likely to pick actual Meowfia
                if (random.nextFloat() < nightSkill) {
                    val meowfia = others.filter { it.alignment == Alignment.MEOWFIA }
                    if (meowfia.isNotEmpty()) meowfia[random.nextInt(meowfia.size)].id
                    else others[random.nextInt(others.size)].id
                } else {
                    others[random.nextInt(others.size)].id
                }
            }
            RoleId.PIGEON, RoleId.CHICKEN -> {
                // Skilled pigeon/chicken targets Farm allies
                if (random.nextFloat() < nightSkill) {
                    val allies = others.filter { it.alignment == Alignment.FARM }
                    if (allies.isNotEmpty()) allies[random.nextInt(allies.size)].id
                    else others[random.nextInt(others.size)].id
                } else {
                    others[random.nextInt(others.size)].id
                }
            }
            RoleId.HOUSE_CAT -> {
                // Skilled cat targets players likely to have eggs
                others[random.nextInt(others.size)].id
            }
            RoleId.TURKEY, RoleId.MOSQUITO, RoleId.TIT, RoleId.BLACK_SWAN -> null
            else -> others[random.nextInt(others.size)].id
        }
    }

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

    /** Choose which cards to throw and which to keep. */
    fun chooseThrow(
        hand: List<SimCard>,
        confidence: Float,
        random: RandomProvider
    ): ThrowDecision {
        if (hand.isEmpty()) return ThrowDecision(emptyList(), emptyList())

        val throwCount = maxOf(1, (hand.size * aggression).toInt())

        // Sort by suit priority based on savvy
        val sorted = if (random.nextFloat() < suitSavvy) {
            hand.sortedBy { card ->
                when (card.suit) {
                    Suit.WILD -> 0     // best to throw
                    Suit.HEARTS -> 1   // safe
                    Suit.SPADES -> 2   // moderate risk
                    Suit.DIAMONDS -> if (confidence > 0.6f) 3 else 6
                    Suit.CLUBS -> if (confidence > 0.7f) 4 else 7
                }
            }
        } else {
            random.shuffle(hand)
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
