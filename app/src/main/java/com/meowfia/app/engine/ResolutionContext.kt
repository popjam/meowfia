package com.meowfia.app.engine

import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.NightResult
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.util.RandomProvider

/**
 * Mutable workspace used during night resolution.
 * Role handlers write into it; the engine reads from it afterward.
 */
class ResolutionContext(
    val players: List<Player>,
    val pool: List<PoolCard>,
    val visitGraph: Map<Int, Int?>,
    val random: RandomProvider
) {
    companion object {
        const val MAX_EGG_DELTA = 5
    }

    private val eggDeltas = mutableMapOf<Int, Int>()
    private val info = mutableMapOf<Int, MutableList<String>>()
    private val effects = mutableMapOf<Int, MutableSet<StatusEffect>>()
    private val narrativeLog = mutableListOf<String>()

    // --- Write operations (called by role handlers) ---

    fun addEggs(playerId: Int, count: Int) {
        eggDeltas[playerId] = (eggDeltas[playerId] ?: 0) + count
    }

    fun removeEggs(playerId: Int, count: Int) {
        eggDeltas[playerId] = (eggDeltas[playerId] ?: 0) - count
    }

    fun addInfo(playerId: Int, text: String) {
        info.getOrPut(playerId) { mutableListOf() }.add(text)
    }

    fun applyEffect(playerId: Int, effect: StatusEffect) {
        effects.getOrPut(playerId) { mutableSetOf() }.add(effect)
    }

    fun log(message: String) {
        narrativeLog.add(message)
    }

    // --- Read operations (called by role handlers and engine) ---

    /** Returns all players whose visit target is [playerId]. */
    fun getVisitorsOf(playerId: Int): List<Player> {
        return visitGraph.entries
            .filter { it.value == playerId }
            .mapNotNull { entry -> players.find { it.id == entry.key } }
    }

    /** Returns the player that [playerId] is visiting, or null if not visiting anyone. */
    fun getVisitTargetOf(playerId: Int): Player? {
        val targetId = visitGraph[playerId] ?: return null
        return players.find { it.id == targetId }
    }

    fun isHugged(playerId: Int): Boolean {
        return effects[playerId]?.contains(StatusEffect.HUGGED) == true
    }

    fun isConfused(playerId: Int): Boolean {
        return effects[playerId]?.contains(StatusEffect.CONFUSED) == true
    }

    /** Returns the egg delta for a player, clamped to -MAX_EGG_DELTA..MAX_EGG_DELTA. */
    fun getClampedEggDelta(playerId: Int): Int {
        return (eggDeltas[playerId] ?: 0).coerceIn(-MAX_EGG_DELTA, MAX_EGG_DELTA)
    }

    fun getInfoFor(playerId: Int): List<String> {
        return info[playerId]?.toList() ?: emptyList()
    }

    fun getEggDelta(playerId: Int): Int {
        return eggDeltas[playerId] ?: 0
    }

    fun getNarrativeLog(): List<String> = narrativeLog.toList()

    // --- Build final results ---

    fun buildNightResults(): Map<Int, NightResult> {
        return players.associate { player ->
            player.id to NightResult(
                playerId = player.id,
                eggDeltas = eggDeltas.toMap(),
                informationGained = getInfoFor(player.id),
                statusApplied = effects.flatMap { (pid, effs) ->
                    effs.map { pid to it }
                },
                narrative = narrativeLog.joinToString("\n")
            )
        }
    }

    fun buildDawnReports(): Map<Int, DawnReport> {
        return players.associate { player ->
            val actual = getClampedEggDelta(player.id)
            val reported = if (isConfused(player.id)) {
                val offset = random.nextInt(1, 3) * if (random.nextFloat() > 0.5f) 1 else -1
                (actual + offset).coerceIn(-MAX_EGG_DELTA, MAX_EGG_DELTA)
            } else {
                actual
            }

            player.id to DawnReport(
                playerId = player.id,
                reportedEggDelta = reported,
                actualEggDelta = actual,
                additionalInfo = getInfoFor(player.id),
                isConfused = isConfused(player.id)
            )
        }
    }
}
