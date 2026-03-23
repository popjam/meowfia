package com.meowfia.app.engine

import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.GamePhase
import com.meowfia.app.data.model.GameState
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.PostRoundAnalysis
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.flowers.FlowerTiming
import com.meowfia.app.util.RandomProvider

/**
 * Central orchestrator for all phase transitions. Holds the current [GameState]
 * and serves as the single source of truth.
 */
class GameCoordinator(
    private val random: RandomProvider = RandomProvider()
) {
    private var _state: GameState? = null
    val state: GameState get() = _state ?: error("Game not started")

    private val nightResolver = NightResolver(random)
    private val roleAssigner = RoleAssigner(random)
    private val dawnReportGenerator = DawnReportGenerator()

    private var resolvedContext: ResolutionContext? = null

    // --- Phase 1: Pool Setup ---

    fun startNewRound(
        roundNumber: Int,
        poolCards: List<PoolCard>,
        playerNames: List<String>,
        dealerSeat: Int
    ): GameState {
        val players = playerNames.mapIndexed { index, name ->
            Player(id = index, name = name)
        }

        val activeFlowers = poolCards
            .filter { it.roleId.isFlower }
            .map { it.roleId }

        _state = GameState(
            roundNumber = roundNumber,
            players = players,
            pool = poolCards,
            dealerSeat = dealerSeat,
            activeFlowers = activeFlowers,
            phase = GamePhase.POOL_SETUP
        )

        // Apply ON_REVEAL flower effects
        for (flowerId in activeFlowers) {
            val handler = FlowerRegistry.get(flowerId) ?: continue
            if (handler.timing == FlowerTiming.ON_REVEAL) {
                _state = handler.activate(state)
            }
        }

        return state
    }

    // --- Phase 2: Registration & Assignment ---

    fun assignRoles(
        forcedAlignments: Map<Int, Alignment>? = null,
        forcedRoles: Map<Int, RoleId>? = null
    ): List<PlayerAssignment> {
        val assignments = roleAssigner.assign(
            playerCount = state.players.size,
            pool = state.pool,
            forcedAlignments = forcedAlignments,
            forcedRoles = forcedRoles,
            activeFlowers = state.activeFlowers
        )

        val updatedPlayers = state.players.map { player ->
            val assignment = assignments.find { it.playerId == player.id }!!
            player.copy(
                alignment = assignment.alignment,
                roleId = assignment.roleId,
                originalRoleId = assignment.roleId
            )
        }

        // Re-apply ON_REVEAL flower effects (e.g. Wolfsbane) now that alignments are set
        var newState = state.copy(
            players = updatedPlayers,
            phase = GamePhase.ROLE_REVEAL
        )
        for (flowerId in state.activeFlowers) {
            val handler = FlowerRegistry.get(flowerId) ?: continue
            if (handler.timing == FlowerTiming.ON_REVEAL) {
                newState = handler.activate(newState)
            }
        }
        _state = newState

        return assignments
    }

    // --- Phase 3: Night ---

    fun submitNightAction(playerId: Int, action: NightAction) {
        val updatedActions = state.nightActions.toMutableMap()
        updatedActions[playerId] = action

        // Build visit graph entry
        val updatedVisitGraph = state.visitGraph.toMutableMap()
        updatedVisitGraph[playerId] = when (action) {
            is NightAction.VisitPlayer -> action.targetId
            is NightAction.VisitSelf -> playerId
            is NightAction.VisitRandom -> null // resolved in resolveAutoTargets
            is NightAction.NoVisit -> null
        }

        _state = state.copy(
            nightActions = updatedActions,
            visitGraph = updatedVisitGraph,
            phase = GamePhase.NIGHT
        )
    }

    fun allNightActionsSubmitted(): Boolean {
        return state.nightActions.size == state.players.size
    }

    fun resolveNight(): GameState {
        // Resolve auto-targets before running the resolver
        resolveAutoTargets()

        resolvedContext = nightResolver.resolve(state)

        // Apply flower pre-resolution effects (e.g. Wolfsbane +1 egg for Meowfia)
        for (flowerId in state.activeFlowers) {
            val handler = FlowerRegistry.get(flowerId) ?: continue
            handler.applyPreResolution(resolvedContext!!, state)
        }

        val nightResults = resolvedContext!!.buildNightResults()

        _state = state.copy(
            nightResults = nightResults,
            phase = GamePhase.DAWN
        )

        // Apply role/alignment modifications from resolution (Frog, Switcheroo, Sheep, etc.)
        val finalRoles = resolvedContext!!.computeFinalRoles()
        val finalAlignments = resolvedContext!!.computeFinalAlignments()
        if (finalRoles.isNotEmpty() || finalAlignments.isNotEmpty()) {
            val swappedPlayers = state.players.map { player ->
                val newRole = finalRoles[player.id]
                val newAlignment = finalAlignments[player.id]
                when {
                    newRole != null && newAlignment != null ->
                        player.copy(roleId = newRole, alignment = newAlignment)
                    newRole != null -> player.copy(roleId = newRole)
                    newAlignment != null -> player.copy(alignment = newAlignment)
                    else -> player
                }
            }
            _state = state.copy(players = swappedPlayers)
        }

        return state
    }

    /** Resolves random targets for Mosquito and Tit before night resolution. */
    private fun resolveAutoTargets() {
        val updatedVisitGraph = state.visitGraph.toMutableMap()

        for (player in state.players) {
            val action = state.nightActions[player.id] ?: continue

            if (action is NightAction.VisitRandom) {
                when (player.roleId) {
                    RoleId.MOSQUITO -> {
                        val others = state.players.filter { it.id != player.id }
                        if (others.isNotEmpty()) {
                            updatedVisitGraph[player.id] = others[random.nextInt(others.size)].id
                        }
                    }
                    RoleId.TIT -> {
                        val meowfiaPlayers = state.players.filter {
                            it.id != player.id && it.alignment == Alignment.MEOWFIA
                        }
                        updatedVisitGraph[player.id] = if (meowfiaPlayers.isNotEmpty()) {
                            meowfiaPlayers[random.nextInt(meowfiaPlayers.size)].id
                        } else {
                            null
                        }
                    }
                    else -> {
                        // Generic random: pick any other player
                        val others = state.players.filter { it.id != player.id }
                        if (others.isNotEmpty()) {
                            updatedVisitGraph[player.id] = others[random.nextInt(others.size)].id
                        }
                    }
                }
            }
        }

        _state = state.copy(visitGraph = updatedVisitGraph)
    }

    // --- Phase 4: Dawn ---

    fun getDawnReport(playerId: Int): DawnReport {
        val ctx = resolvedContext ?: error("Night must be resolved before generating dawn reports")
        val reports = dawnReportGenerator.generate(state.players, ctx, state.activeFlowers)
        val report = reports[playerId] ?: error("No dawn report for player $playerId")

        // Store in state
        val updatedReports = state.dawnReports.toMutableMap()
        updatedReports[playerId] = report
        _state = state.copy(dawnReports = updatedReports)

        return report
    }

    // --- Phase 5: Day ---

    fun startDay(): GameState {
        _state = state.copy(phase = GamePhase.DAY)
        return state
    }

    fun recordCawCaw(): GameState {
        _state = state.copy(cawCawCount = state.cawCawCount + 1)
        return state
    }

    fun isCawCawTriggered(): Boolean = state.cawCawCount >= 3

    // --- Phase 6: Voting Result ---

    fun recordEggsecution(eliminatedPlayerId: Int): GameState {
        _state = state.copy(
            eliminatedPlayerId = eliminatedPlayerId,
            phase = GamePhase.VOTING_RESULT
        )
        return state
    }

    /** If eliminated is Meowfia → Farm wins. If Farm → Meowfia wins. */
    fun getWinningTeam(): Alignment? {
        val eliminatedId = state.eliminatedPlayerId ?: return null
        val eliminated = state.players.find { it.id == eliminatedId } ?: return null
        return when (eliminated.alignment) {
            Alignment.MEOWFIA -> Alignment.FARM
            Alignment.FARM -> Alignment.MEOWFIA
        }
    }

    // --- Moonflower support ---

    /**
     * Clears night actions and visit graph so a second night phase can collect
     * fresh actions from all players.
     */
    fun clearNightActions() {
        _state = state.copy(
            nightActions = emptyMap(),
            visitGraph = emptyMap()
        )
    }

    /**
     * Patches the current [resolvedContext] with extra egg deltas (e.g. from a
     * prior Moonflower night). Must be called after [resolveNight] and before
     * [getDawnReport].
     */
    fun addExtraEggDeltas(deltas: Map<Int, Int>) {
        val ctx = resolvedContext ?: return
        for ((playerId, delta) in deltas) {
            if (delta > 0) ctx.addEggs(playerId, delta)
            else if (delta < 0) ctx.removeEggs(playerId, -delta)
        }
    }

    // --- Bird of Paradise support ---

    /**
     * Adds an extra bot player mid-round with a randomly chosen alignment and
     * the corresponding buffer role (Pigeon for Farm, House Cat for Meowfia).
     */
    fun addBotPlayer(name: String): Player {
        val newId = state.players.size
        val alignment = if (random.nextFloat() < 1f / 3f) Alignment.MEOWFIA else Alignment.FARM
        val role = if (alignment == Alignment.MEOWFIA) RoleId.HOUSE_CAT else RoleId.PIGEON
        val player = Player(
            id = newId,
            name = name,
            alignment = alignment,
            roleId = role,
            originalRoleId = role,
            isBot = true
        )
        _state = state.copy(players = state.players + player)
        return player
    }

    // --- Post-Round Analysis ---

    /**
     * Builds a detailed [PostRoundAnalysis] walkthrough of the completed round.
     * Must be called after [resolveNight] (requires a resolved context).
     */
    fun getPostRoundAnalysis(): PostRoundAnalysis {
        val ctx = resolvedContext ?: error("Night must be resolved before generating analysis")
        return PostRoundAnalyzer.analyze(state, ctx, getWinningTeam())
    }

    // --- Queries ---

    fun getPlayerCount(): Int = state.players.size
    fun getCurrentPhase(): GamePhase = state.phase

    /** Expose the random provider for bot decisions. */
    val randomProvider: RandomProvider get() = random

    /** Update the player list (e.g. to flag bots after role assignment). */
    fun updatePlayers(players: List<Player>) {
        _state = state.copy(players = players)
    }
}
