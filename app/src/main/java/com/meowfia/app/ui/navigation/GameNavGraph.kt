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
import com.meowfia.app.ui.screens.RoundSummaryScreen
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
    data object PoolViewer : MeowfiaRoute("pool_viewer")
}

@Composable
fun GameNavGraph(navController: NavHostController) {
    var selectedRoles by remember { mutableStateOf(emptyList<RoleId>()) }
    var playerCount by remember { mutableIntStateOf(6) }
    var botCount by remember { mutableIntStateOf(0) }
    var playerNames by remember { mutableStateOf(emptyList<String>()) }
    var botNames by remember { mutableStateOf(emptyList<String>()) }
    var assignments by remember { mutableStateOf(emptyList<PlayerAssignment>()) }
    var currentPlayerIndex by remember { mutableIntStateOf(0) }
    var roundNumber by remember { mutableIntStateOf(1) }
    var cawCawCount by remember { mutableIntStateOf(0) }
    var botClaims by remember { mutableStateOf(emptyList<BotDayClaim>()) }

    NavHost(
        navController = navController,
        startDestination = MeowfiaRoute.Start.route
    ) {
        composable(MeowfiaRoute.Start.route) {
            StartScreen(
                onNewGame = { navController.navigate(MeowfiaRoute.PoolSetup.route) },
                onViewRoles = { navController.navigate(MeowfiaRoute.PoolViewer.route) }
            )
        }

        composable(MeowfiaRoute.PoolSetup.route) {
            PoolSetupScreen(
                onStartGame = { roles, count, bots ->
                    selectedRoles = roles
                    playerCount = count
                    botCount = bots

                    GameSession.startNewGame(botCount = bots)
                    val pool = roles.map { PoolCard(it) }

                    // Generate bot names
                    val generatedBotNames = BotNames.pick(bots, GameSession.coordinator.randomProvider)
                    botNames = generatedBotNames

                    // Placeholder names: humans get "Player N", bots get their unique names
                    val humanCount = count - bots
                    val placeholderNames = (1..humanCount).map { "Player $it" } + generatedBotNames

                    GameSession.coordinator.startNewRound(
                        roundNumber = roundNumber,
                        poolCards = pool,
                        playerNames = placeholderNames,
                        dealerSeat = 0
                    )
                    assignments = GameSession.coordinator.assignRoles()

                    // Flag bot players
                    flagBotPlayers(humanCount)

                    currentPlayerIndex = 0

                    if (humanCount > 0) {
                        navController.navigate(MeowfiaRoute.PlayerRegistration.route)
                    } else {
                        // All bots — skip registration
                        playerNames = placeholderNames
                        navController.navigate(MeowfiaRoute.NightPhase.route)
                    }
                }
            )
        }

        composable(MeowfiaRoute.PlayerRegistration.route) {
            val humanCount = playerCount - botCount
            PlayerRegistrationScreen(
                humanCount = humanCount,
                assignments = assignments,
                onAllRegistered = { names, profiles ->
                    // Combine human names with bot names
                    playerNames = names + botNames
                    GameSession.profileImages.putAll(profiles)

                    // Update the coordinator with real names
                    val pool = selectedRoles.map { PoolCard(it) }
                    GameSession.coordinator.startNewRound(
                        roundNumber = roundNumber,
                        poolCards = pool,
                        playerNames = playerNames,
                        dealerSeat = (roundNumber - 1) % playerNames.size
                    )
                    // Re-assign with same forced alignments/roles to keep assignments stable
                    val forcedAlignments = assignments.associate { it.playerId to it.alignment }
                    val forcedRoles = assignments.associate { it.playerId to it.roleId }
                    assignments = GameSession.coordinator.assignRoles(
                        forcedAlignments = forcedAlignments,
                        forcedRoles = forcedRoles
                    )

                    // Re-flag bot players
                    flagBotPlayers(humanCount)

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
                    GameSession.coordinator.resolveNight()
                    currentPlayerIndex = 0
                    navController.navigate(MeowfiaRoute.DawnPhase.route)
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
                botClaims = botClaims,
                onCawCaw = {
                    GameSession.coordinator.recordCawCaw()
                    cawCawCount = GameSession.coordinator.state.cawCawCount
                },
                onTimeUp = {
                    navController.navigate(MeowfiaRoute.VotingResult.route)
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
                }
            )
        }

        composable(MeowfiaRoute.RoundSummary.route) {
            val state = GameSession.coordinator.state
            RoundSummaryScreen(
                roundNumber = roundNumber,
                players = state.players,
                visitGraph = state.visitGraph,
                dawnReports = state.dawnReports,
                eliminatedPlayerId = state.eliminatedPlayerId,
                winningTeam = GameSession.coordinator.getWinningTeam(),
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

                    // Re-flag bot players
                    val humanCount = playerCount - botCount
                    flagBotPlayers(humanCount)

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

        composable(MeowfiaRoute.PoolViewer.route) {
            PoolViewerScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

/** Flag players with id >= humanCount as bots. */
private fun flagBotPlayers(humanCount: Int) {
    val updatedPlayers = GameSession.coordinator.state.players.map { player ->
        player.copy(isBot = player.id >= humanCount)
    }
    GameSession.coordinator.updatePlayers(updatedPlayers)
}
