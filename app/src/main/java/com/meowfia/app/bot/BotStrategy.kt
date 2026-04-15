package com.meowfia.app.bot

/**
 * Controls a bot's decision-making skill level.
 *
 * @param nightSkill 0.0–1.0 — Probability of making a strategically optimal night target choice.
 *                   0.0 = purely random, 1.0 = always picks the best target.
 */
data class BotStrategy(
    val name: String = "Default",
    val nightSkill: Float = 0.5f
) {
    companion object {
        /** Balanced default for real-game bots. */
        val DEFAULT = BotStrategy(name = "Default", nightSkill = 0.5f)
    }
}
