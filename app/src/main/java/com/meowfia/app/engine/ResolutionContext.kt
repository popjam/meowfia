package com.meowfia.app.engine

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.NightResult
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.RoleModification
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.data.registry.RoleRegistry
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
    private val modifications = mutableListOf<RoleModification>()

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

    /** Exchange roles between two players. Reads their current (post-modification) roles. */
    fun swapRoles(playerIdA: Int, playerIdB: Int) {
        modifications.add(RoleModification.SwapRoles(playerIdA, playerIdB))
    }

    /** Set a player's role to a specific value. */
    fun setRole(playerId: Int, roleId: RoleId) {
        modifications.add(RoleModification.SetRole(playerId, roleId))
    }

    /** Set a player's alignment to a specific value. */
    fun setAlignment(playerId: Int, alignment: Alignment) {
        modifications.add(RoleModification.SetAlignment(playerId, alignment))
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

    fun getModifications(): List<RoleModification> = modifications.toList()

    /** Replays all modifications so far to get a player's current role. */
    fun getCurrentRole(playerId: Int): RoleId {
        var roles = players.associate { it.id to it.roleId }.toMutableMap()
        for (mod in modifications) {
            when (mod) {
                is RoleModification.SwapRoles -> {
                    val roleA = roles[mod.playerIdA]
                    val roleB = roles[mod.playerIdB]
                    if (roleA != null) roles[mod.playerIdB] = roleA
                    if (roleB != null) roles[mod.playerIdA] = roleB
                }
                is RoleModification.SetRole -> roles[mod.playerId] = mod.roleId
                is RoleModification.SetAlignment -> { /* no effect on roles */ }
            }
        }
        return roles[playerId] ?: players.first { it.id == playerId }.roleId
    }

    /** Replays all modifications so far to get a player's current alignment. */
    fun getCurrentAlignment(playerId: Int): Alignment {
        val alignments = players.associate { it.id to it.alignment }.toMutableMap()
        for (mod in modifications) {
            when (mod) {
                is RoleModification.SwapRoles -> {
                    val alignA = alignments[mod.playerIdA]
                    val alignB = alignments[mod.playerIdB]
                    if (alignA != null) alignments[mod.playerIdB] = alignA
                    if (alignB != null) alignments[mod.playerIdA] = alignB
                }
                is RoleModification.SetAlignment -> alignments[mod.playerId] = mod.alignment
                is RoleModification.SetRole -> { /* no effect on alignment */ }
            }
        }
        return alignments[playerId] ?: players.first { it.id == playerId }.alignment
    }

    /**
     * Returns how a player's alignment **appears** to investigators.
     * Accounts for role-specific deception (e.g. Ugly Duckling appears Meowfia)
     * on top of any alignment modifications (e.g. Top Cat flips).
     */
    fun getApparentAlignment(playerId: Int): Alignment {
        val currentRole = getCurrentRole(playerId)
        val handler = RoleRegistry.get(currentRole)
        val player = players.first { it.id == playerId }
        // Build a snapshot reflecting current modifications
        val currentAlignment = getCurrentAlignment(playerId)
        val snapshotPlayer = player.copy(alignment = currentAlignment, roleId = currentRole)
        return handler.getApparentAlignment(snapshotPlayer)
    }

    /** Replays all modifications to compute the final role for each player. */
    fun computeFinalRoles(): Map<Int, RoleId> {
        val roles = players.associate { it.id to it.roleId }.toMutableMap()
        for (mod in modifications) {
            when (mod) {
                is RoleModification.SwapRoles -> {
                    val roleA = roles[mod.playerIdA]
                    val roleB = roles[mod.playerIdB]
                    if (roleA != null) roles[mod.playerIdB] = roleA
                    if (roleB != null) roles[mod.playerIdA] = roleB
                }
                is RoleModification.SetRole -> roles[mod.playerId] = mod.roleId
                is RoleModification.SetAlignment -> { /* no effect on roles */ }
            }
        }
        // Only return entries that actually changed
        return roles.filter { (id, role) ->
            players.find { it.id == id }?.roleId != role
        }
    }

    /** Replays all modifications to compute the final alignment for each player.
     *  Role swaps also swap alignments — the alignment stays with the role, not the player. */
    fun computeFinalAlignments(): Map<Int, Alignment> {
        val alignments = players.associate { it.id to it.alignment }.toMutableMap()
        for (mod in modifications) {
            when (mod) {
                is RoleModification.SwapRoles -> {
                    val alignA = alignments[mod.playerIdA]
                    val alignB = alignments[mod.playerIdB]
                    if (alignA != null) alignments[mod.playerIdB] = alignA
                    if (alignB != null) alignments[mod.playerIdA] = alignB
                }
                is RoleModification.SetAlignment -> alignments[mod.playerId] = mod.alignment
                is RoleModification.SetRole -> { /* no effect on alignment */ }
            }
        }
        // Only return entries that actually changed
        return alignments.filter { (id, alignment) ->
            players.find { it.id == id }?.alignment != alignment
        }
    }

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
