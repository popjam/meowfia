package com.meowfia.app.testing.sim

import com.meowfia.app.util.RandomProvider

/** Six standard player archetypes covering the strategy space. */
object SimStrategyArchetypes {

    val AGGRESSIVE_SKILLED = SimStrategy(
        name = "Aggressive-Skilled",
        nightSkill = 0.80f, deduction = 0.80f,
        aggression = 0.85f, suitSavvy = 0.75f
    )

    val AGGRESSIVE_UNSKILLED = SimStrategy(
        name = "Aggressive-Unskilled",
        nightSkill = 0.20f, deduction = 0.20f,
        aggression = 0.85f, suitSavvy = 0.25f
    )

    val CONSERVATIVE_SKILLED = SimStrategy(
        name = "Conservative-Skilled",
        nightSkill = 0.80f, deduction = 0.80f,
        aggression = 0.20f, suitSavvy = 0.85f
    )

    val CONSERVATIVE_UNSKILLED = SimStrategy(
        name = "Conservative-Unskilled",
        nightSkill = 0.20f, deduction = 0.20f,
        aggression = 0.20f, suitSavvy = 0.20f
    )

    val BALANCED_SKILLED = SimStrategy(
        name = "Balanced-Skilled",
        nightSkill = 0.70f, deduction = 0.70f,
        aggression = 0.50f, suitSavvy = 0.65f
    )

    val BALANCED_RANDOM = SimStrategy(
        name = "Balanced-Random",
        nightSkill = 0.35f, deduction = 0.35f,
        aggression = 0.50f, suitSavvy = 0.35f
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
}
