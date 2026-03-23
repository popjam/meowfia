package com.meowfia.app.util

/**
 * Wrapper around [kotlin.random.Random] that supports seeded reproducibility.
 * All game randomness flows through this class so tests can replay exact sequences.
 */
class RandomProvider(val seed: Long = System.currentTimeMillis()) {
    private val random = kotlin.random.Random(seed)

    fun nextFloat(): Float = random.nextFloat()
    fun nextInt(until: Int): Int = random.nextInt(until)
    fun nextInt(from: Int, until: Int): Int = random.nextInt(from, until)

    fun <T> shuffle(list: List<T>): List<T> = list.shuffled(random)
}
