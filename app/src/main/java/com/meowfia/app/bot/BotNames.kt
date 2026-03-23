package com.meowfia.app.bot

import com.meowfia.app.util.RandomProvider

object BotNames {
    private val NAMES = listOf(
        "Botsworth",
        "Robo-kit",
        "Botpurr",
        "Sir Botsworth III",
        "Meowbot",
        "Botwhisker",
        "Furbotnik",
        "Bottington",
        "Botpaws",
        "Scatterbot",
        "Botfluff",
        "Botbean",
        "Catnipbot",
        "Botmuffin",
        "Yarnbot",
        "Botclaw"
    )

    fun pick(count: Int, random: RandomProvider): List<String> {
        val shuffled = random.shuffle(NAMES)
        return (0 until count).map { i ->
            if (i < shuffled.size) shuffled[i]
            else "${shuffled[i % shuffled.size]} ${i / shuffled.size + 1}"
        }
    }
}
