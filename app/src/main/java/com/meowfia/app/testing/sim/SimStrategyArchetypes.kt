package com.meowfia.app.testing.sim

import com.meowfia.app.util.RandomProvider

/** Six standard player archetypes covering the strategy space. */
object SimStrategyArchetypes {

    val AGGRESSIVE_SKILLED = SimStrategy(
        name = "Aggressive-Skilled",
        nightSkill = 0.80f, deduction = 0.80f,
        aggression = 0.85f
    )

    val AGGRESSIVE_UNSKILLED = SimStrategy(
        name = "Aggressive-Unskilled",
        nightSkill = 0.20f, deduction = 0.20f,
        aggression = 0.85f
    )

    val CONSERVATIVE_SKILLED = SimStrategy(
        name = "Conservative-Skilled",
        nightSkill = 0.80f, deduction = 0.80f,
        aggression = 0.20f
    )

    val CONSERVATIVE_UNSKILLED = SimStrategy(
        name = "Conservative-Unskilled",
        nightSkill = 0.20f, deduction = 0.20f,
        aggression = 0.20f
    )

    val BALANCED_SKILLED = SimStrategy(
        name = "Balanced-Skilled",
        nightSkill = 0.70f, deduction = 0.70f,
        aggression = 0.50f
    )

    val BALANCED_RANDOM = SimStrategy(
        name = "Balanced-Random",
        nightSkill = 0.35f, deduction = 0.35f,
        aggression = 0.50f
    )

    val ALL = listOf(
        AGGRESSIVE_SKILLED, AGGRESSIVE_UNSKILLED,
        CONSERVATIVE_SKILLED, CONSERVATIVE_UNSKILLED,
        BALANCED_SKILLED, BALANCED_RANDOM
    )

    /** Assign strategies to N players by cycling through shuffled archetypes. */
    fun assignToPlayers(count: Int, random: RandomProvider): List<SimStrategy> {
        val shuffled = random.shuffle(ALL)
        return (0 until count).map { shuffled[it % shuffled.size] }
    }

    /** Assign strategies based on a distribution preset. */
    fun assignForDistribution(
        count: Int,
        distribution: StrategyDistribution,
        random: RandomProvider
    ): List<SimStrategy> {
        val pool = when (distribution) {
            StrategyDistribution.BALANCED -> return assignToPlayers(count, random)
            StrategyDistribution.ALL_SKILLED -> listOf(AGGRESSIVE_SKILLED, CONSERVATIVE_SKILLED, BALANCED_SKILLED)
            StrategyDistribution.ALL_UNSKILLED -> listOf(AGGRESSIVE_UNSKILLED, CONSERVATIVE_UNSKILLED, BALANCED_RANDOM)
            StrategyDistribution.ALL_AGGRESSIVE -> listOf(AGGRESSIVE_SKILLED, AGGRESSIVE_UNSKILLED)
            StrategyDistribution.ALL_CONSERVATIVE -> listOf(CONSERVATIVE_SKILLED, CONSERVATIVE_UNSKILLED)
            StrategyDistribution.RANDOM -> {
                return (0 until count).map {
                    SimStrategy(
                        name = "Random-${it + 1}",
                        nightSkill = random.nextFloat(),
                        deduction = random.nextFloat(),
                        aggression = random.nextFloat()
                    )
                }
            }
        }
        val shuffled = random.shuffle(pool)
        return (0 until count).map { shuffled[it % shuffled.size] }
    }
}
