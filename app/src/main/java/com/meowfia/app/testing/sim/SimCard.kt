package com.meowfia.app.testing.sim

import com.meowfia.app.util.RandomProvider

enum class Suit(val symbol: String) {
    HEARTS("\u2665"),
    DIAMONDS("\u2666"),
    CLUBS("\u2663"),
    SPADES("\u2660"),
    WILD("\u2605")
}

data class SimCard(
    val suit: Suit,
    val value: Int
) {
    val display: String
        get() = when (suit) {
            Suit.WILD -> "Wild"
            else -> "$value${suit.symbol}"
        }
}

/** Builds the standard 64-card Meowfia deck. */
object SimDeck {
    fun create(random: RandomProvider): MutableList<SimCard> {
        val deck = mutableListOf<SimCard>()
        for (suit in listOf(Suit.HEARTS, Suit.DIAMONDS, Suit.CLUBS, Suit.SPADES)) {
            for (v in 1..13) deck.add(SimCard(suit, v))
            deck.add(SimCard(suit, random.nextInt(1, 14)))
        }
        repeat(8) { deck.add(SimCard(Suit.WILD, 0)) }
        return random.shuffle(deck).toMutableList()
    }
}
