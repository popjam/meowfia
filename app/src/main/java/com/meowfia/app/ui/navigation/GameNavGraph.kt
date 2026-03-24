package com.meowfia.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.meowfia.app.bot.BotClaimGenerator
import com.meowfia.app.bot.BotDayClaim
import com.meowfia.app.bot.BotNames
import com.meowfia.app.ui.components.generateColorProfile
import com.meowfia.app.data.model.PreConfiguredPlayer
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.engine.GameSession
import com.meowfia.app.ui.screens.DawnPhaseScreen
import com.meowfia.app.ui.screens.DayTimerScreen
import com.meowfia.app.ui.screens.NightPhaseScreen
import com.meowfia.app.ui.screens.PlayerRegistrationScreen
import com.meowfia.app.ui.screens.PoolSetupScreen
import com.meowfia.app.ui.screens.PoolViewerScreen
import com.meowfia.app.ui.screens.PostRoundAnalysisScreen
import com.meowfia.app.ui.screens.RoundSummaryScreen
import com.meowfia.app.ui.screens.SimulationScreen
import com.meowfia.app.ui.screens.StartScreen
import com.meowfia.app.ui.screens.VotingResultScreen

sealed class MeowfiaRoute(val route: String) {
    data object Start : MeowfiaRoute("start")
    data object PoolSetup : MeowfiaRoute("pool_setup")
    data object PlayerRegistration : MeowfiaRoute("player_registration")
    data object NightPhase : MeowfiaRoute("night_phase")
    data object DawnPhase : MeowfiaRoute("dawn_phase")
    data object DayTimer : MeowfiaRoute("day_timer")
    data object VotingResult : MeowfiaRoute("voting_result")
    data object RoundSummary : MeowfiaRoute("round_summary")
    data object PostRoundAnalysis : MeowfiaRoute("post_round_analysis")
    data object PoolViewer : MeowfiaRoute("pool_viewer")
    data object Simulation : MeowfiaRoute("simulation")
}

@Composable
fun GameNavGraph(navController: NavHostController) {
    var selectedRoles by remember { mutableStateOf(emptyList<RoleId>()) }
    var storedPlayerSlots by remember { mutableStateOf(emptyList<PreConfiguredPlayer>()) }
    var playerCount by remember { mutableIntStateOf(6) }
    var botCount by remember { mutableIntStateOf(0) }
    var playerNames by remember { mutableStateOf(emptyList<String>()) }
    var botNames by remember { mutableStateOf(emptyList<String>()) }
    var assignments by remember { mutableStateOf(emptyList<PlayerAssignment>()) }
    var currentPlayerIndex by remember { mutableIntStateOf(0) }
    var roundNumber by remember { mutableIntStateOf(1) }
    var cawCawCount by remember { mutableIntStateOf(0) }
    var botClaims by remember { mutableStateOf(emptyList<BotDayClaim>()) }
    var nightPhaseCount by remember { mutableIntStateOf(0) }
    var firstNightDeltas by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    NavHost(
        navController = navController,
        startDestination = MeowfiaRoute.Start.route
    ) {
        composable(MeowfiaRoute.Start.route) {
            StartScreen(
                onNewGame = { navController.navigate(MeowfiaRoute.PoolSetup.route) },
                onViewRoles = { navController.navigate(MeowfiaRoute.PoolViewer.route) },
                onSimulation = { navController.navigate(MeowfiaRoute.Simulation.route) }
            )
        }

        composable(MeowfiaRoute.PoolSetup.route) {
            PoolSetupScreen(
                onStartGame = { roles, slots ->
                    selectedRoles = roles
                    storedPlayerSlots = slots
                    playerCount = slots.size
                    botCount = slots.count { it.isBot }
                    val humanCount = playerCount - botCount

                    GameSession.startNewGame(botCount = botCount)
                    val pool = roles.map { PoolCard(it) }

                    // Generate bot names for slots that are bots without names
                    val generatedBotNames = BotNames.pick(botCount, GameSession.coordinator.randomProvider)
                    botNames = generatedBotNames

                    // Build player names from slots
                    var botNameIdx = 0
                    var humanIdx = 0
                    val allNames = slots.mapIndexed { index, slot ->
                        when {
                            slot.isBot -> slot.name ?: generatedBotNames.getOrElse(botNameIdx++) { "Bot ${index + 1}" }
                            slot.name != null -> slot.name
                            else -> { humanIdx++; "Player $humanIdx" }
                        }
                    }
                    playerNames = allNames

                    GameSession.coordinator.startNewRound(
                        roundNumber = roundNumber,
                        poolCards = pool,
                        playerNames = allNames,
                        dealerSeat = 0
                    )
                    assignments = GameSession.coordinator.assignRoles()

                    // Bird of Paradise
                    if (RoleId.BIRD_OF_PARADISE in GameSession.coordinator.state.activeFlowers) {
                        val botPlayer = GameSession.coordinator.addBotPlayer("Paradise Bot")
                        GameSession.profileImages[botPlayer.id] = generateColorProfile(botPlayer.id)
                    }

                    // Flag bots and assign profiles
                    flagBotPlayers(slots)
                    slots.forEachIndexed { index, slot ->
                        if (slot.profileBitmap != null) {
                            GameSession.profileImages[index] = slot.profileBitmap
                        } else if (slot.isBot || GameSession.profileImages[index] == null) {
                            GameSession.profileImages[index] = generateColorProfile(index)
                        }
                    }

                    currentPlayerIndex = 0

                    if (humanCount > 0) {
                        navController.navigate(MeowfiaRoute.PlayerRegistration.route)
                    } else {
                        navController.navigate(MeowfiaRoute.NightPhase.route)
                    }
                }
            )
        }

        composable(MeowfiaRoute.PlayerRegistration.route) {
            val humanIndices = storedPlayerSlots.indices.filter { !storedPlayerSlots[it].isBot }
            // Build human-only name list for display
            val humanNames = humanIndices.map { playerNames.getOrElse(it) { "Player ${it + 1}" } }
            PlayerRegistrationScreen(
                humanCount = humanIndices.size,
                assignments = assignments,
                playerNames = humanNames,
                onAllDone = {
                    currentPlayerIndex = 0
                    navController.navigate(MeowfiaRoute.NightPhase.route)
                }
            )
        }

        composable(MeowfiaRoute.NightPhase.route) {
            NightPhaseScreen(
                roundNumber = roundNumber,
                players = GameSession.coordinator.state.players,
                currentPlayerIndex = currentPlayerIndex,
                onActionSubmitted = { playerId, action ->
                    GameSession.coordinator.submitNightAction(playerId, action)
                    currentPlayerIndex++
                },
                onAllDone = {
                    val activeFlowers = GameSession.coordinator.state.activeFlowers
                    if (RoleId.MOONFLOWER in activeFlowers && nightPhaseCount == 0) {
                        // First night of Moonflower double-night
                        GameSession.coordinator.resolveNight()
                        // Save first night egg deltas
                        firstNightDeltas = GameSession.coordinator.state.players.associate { p ->
                            p.id to (GameSession.coordinator.state.nightResults[p.id]
                                ?.eggDeltas?.get(p.id) ?: 0)
                        }
                        // Clear actions for second night pass
                        GameSession.coordinator.clearNightActions()
                        nightPhaseCount = 1
                        currentPlayerIndex = 0
                        navController.navigate(MeowfiaRoute.NightPhase.route) {
                            popUpTo(MeowfiaRoute.NightPhase.route) { inclusive = true }
                        }
                    } else {
                        // Normal night or second Moonflower night
                        GameSession.coordinator.resolveNight()
                        if (nightPhaseCount == 1) {
                            // Patch second night's context with first night's egg deltas
                            GameSession.coordinator.addExtraEggDeltas(firstNightDeltas)
                            firstNightDeltas = emptyMap()
                        }
                        nightPhaseCount = 0
                        currentPlayerIndex = 0
                        navController.navigate(MeowfiaRoute.DawnPhase.route)
                    }
                }
            )
        }

        composable(MeowfiaRoute.DawnPhase.route) {
            DawnPhaseScreen(
                roundNumber = roundNumber,
                players = GameSession.coordinator.state.players,
                currentPlayerIndex = currentPlayerIndex,
                getDawnReport = { playerId ->
                    GameSession.coordinator.getDawnReport(playerId)
                },
                onPlayerDone = {
                    currentPlayerIndex++
                },
                onAllDone = {
                    GameSession.coordinator.startDay()
                    cawCawCount = 0

                    // Generate bot claims
                    val state = GameSession.coordinator.state
                    val botPlayers = state.players.filter { it.isBot }
                    botClaims = botPlayers.map { bot ->
                        BotClaimGenerator.generateClaim(
                            bot = bot,
                            allPlayers = state.players,
                            dawnReport = state.dawnReports[bot.id]!!,
                            visitGraph = state.visitGraph,
                            pool = state.pool.map { it.roleId }.filter { it.isFarmAnimal },
                            random = GameSession.coordinator.randomProvider
                        )
                    }

                    navController.navigate(MeowfiaRoute.DayTimer.route)
                }
            )
        }

        composable(MeowfiaRoute.DayTimer.route) {
            DayTimerScreen(
                roundNumber = roundNumber,
                cawCawCount = cawCawCount,
                activeFlowers = GameSession.coordinator.state.activeFlowers,
                players = GameSession.coordinator.state.players,
                botClaims = botClaims,
                visitGraph = GameSession.coordinator.state.visitGraph,
                onCawCaw = {
                    GameSession.coordinator.recordCawCaw()
                    cawCawCount = GameSession.coordinator.state.cawCawCount
                },
                onTimeUp = {
                    navController.navigate(MeowfiaRoute.VotingResult.route)
                },
                onSkipRound = {
                    // Cactus Flower: skip entire day+vote when zero Meowfia
                    roundNumber++
                    currentPlayerIndex = 0
                    cawCawCount = 0

                    val pool = selectedRoles.map { PoolCard(it) }
                    GameSession.coordinator.startNewRound(
                        roundNumber = roundNumber,
                        poolCards = pool,
                        playerNames = playerNames,
                        dealerSeat = (roundNumber - 1) % playerNames.size
                    )
                    assignments = GameSession.coordinator.assignRoles()

                    if (RoleId.BIRD_OF_PARADISE in GameSession.coordinator.state.activeFlowers) {
                        val botPlayer = GameSession.coordinator.addBotPlayer("Paradise Bot")
                        GameSession.profileImages[botPlayer.id] = generateColorProfile(botPlayer.id)
                    }

                    flagBotPlayers(storedPlayerSlots)

                    navController.navigate(MeowfiaRoute.NightPhase.route) {
                        popUpTo(MeowfiaRoute.Start.route) { inclusive = false }
                    }
                }
            )
        }

        composable(MeowfiaRoute.VotingResult.route) {
            val state = GameSession.coordinator.state
            val eliminatedPlayer = state.eliminatedPlayerId?.let { id ->
                state.players.find { it.id == id }
            }
            val winningTeam = GameSession.coordinator.getWinningTeam()

            VotingResultScreen(
                players = state.players,
                onEggsecuted = { eliminatedId ->
                    GameSession.coordinator.recordEggsecution(eliminatedId)
                    navController.navigate(MeowfiaRoute.VotingResult.route) {
                        popUpTo(MeowfiaRoute.VotingResult.route) { inclusive = true }
                    }
                },
                eliminatedPlayer = eliminatedPlayer,
                winningTeam = winningTeam,
                onViewSummary = {
                    navController.navigate(MeowfiaRoute.RoundSummary.route)
                },
                profileImages = GameSession.profileImages
            )
        }

        composable(MeowfiaRoute.RoundSummary.route) {
            // Capture summary data once so it's not affected by next-round state mutations
            val summaryData = remember {
                val state = GameSession.coordinator.state
                SummarySnapshot(
                    players = state.players,
                    visitGraph = state.visitGraph,
                    dawnReports = state.dawnReports,
                    eliminatedPlayerId = state.eliminatedPlayerId,
                    winningTeam = GameSession.coordinator.getWinningTeam()
                )
            }
            RoundSummaryScreen(
                roundNumber = roundNumber,
                players = summaryData.players,
                visitGraph = summaryData.visitGraph,
                dawnReports = summaryData.dawnReports,
                eliminatedPlayerId = summaryData.eliminatedPlayerId,
                winningTeam = summaryData.winningTeam,
                onViewAnalysis = {
                    navController.navigate(MeowfiaRoute.PostRoundAnalysis.route)
                },
                onNextRound = {
                    roundNumber++
                    currentPlayerIndex = 0
                    cawCawCount = 0

                    // Start new round
                    val pool = selectedRoles.map { PoolCard(it) }
                    val placeholderNames = playerNames // reuse names from last round
                    GameSession.coordinator.startNewRound(
                        roundNumber = roundNumber,
                        poolCards = pool,
                        playerNames = placeholderNames,
                        dealerSeat = (roundNumber - 1) % placeholderNames.size
                    )
                    assignments = GameSession.coordinator.assignRoles()

                    // Bird of Paradise: add an extra bot player
                    if (RoleId.BIRD_OF_PARADISE in GameSession.coordinator.state.activeFlowers) {
                        val botPlayer = GameSession.coordinator.addBotPlayer("Paradise Bot")
                        GameSession.profileImages[botPlayer.id] = generateColorProfile(botPlayer.id)
                    }

                    // Re-flag bot players
                    flagBotPlayers(storedPlayerSlots)

                    // For subsequent rounds, skip registration — go straight to night
                    currentPlayerIndex = 0
                    navController.navigate(MeowfiaRoute.NightPhase.route) {
                        popUpTo(MeowfiaRoute.Start.route) { inclusive = false }
                    }
                },
                onEndGame = {
                    roundNumber = 1
                    navController.navigate(MeowfiaRoute.Start.route) {
                        popUpTo(MeowfiaRoute.Start.route) { inclusive = true }
                    }
                }
            )
        }

        composable(MeowfiaRoute.PostRoundAnalysis.route) {
            val analysis = remember { GameSession.coordinator.getPostRoundAnalysis() }
            PostRoundAnalysisScreen(
                analysis = analysis,
                onBack = { navController.popBackStack() }
            )
        }

        composable(MeowfiaRoute.PoolViewer.route) {
            PoolViewerScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(MeowfiaRoute.Simulation.route) {
            SimulationScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/** Flag players as bots based on their slot configuration. */
private fun flagBotPlayers(slots: List<PreConfiguredPlayer>) {
    val updatedPlayers = GameSession.coordinator.state.players.map { player ->
        player.copy(isBot = slots.getOrNull(player.id)?.isBot == true)
    }
    GameSession.coordinator.updatePlayers(updatedPlayers)
}

/** Frozen snapshot of round summary data to prevent leaking next-round info. */
private data class SummarySnapshot(
    val players: List<com.meowfia.app.data.model.Player>,
    val visitGraph: Map<Int, Int?>,
    val dawnReports: Map<Int, com.meowfia.app.data.model.DawnReport>,
    val eliminatedPlayerId: Int?,
    val winningTeam: com.meowfia.app.data.model.Alignment?
)
