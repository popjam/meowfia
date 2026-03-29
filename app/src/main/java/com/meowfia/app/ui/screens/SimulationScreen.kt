package com.meowfia.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment as GameAlignment
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.flowers.FlowerRegistry
import com.meowfia.app.testing.analysis.BatchRunner
import com.meowfia.app.testing.analysis.BatchStatistics
import com.meowfia.app.testing.sim.Consolation
import com.meowfia.app.testing.sim.LossThrownAction
import com.meowfia.app.testing.sim.ScoringRules
import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimEngine
import com.meowfia.app.testing.sim.SimGameResult
import com.meowfia.app.testing.sim.SimRoundLog
import com.meowfia.app.testing.sim.StrategyDistribution
import com.meowfia.app.testing.sim.TieBreaker
import com.meowfia.app.testing.sim.Verbosity
import com.meowfia.app.testing.sim.WinThrownAction
import com.meowfia.app.testing.sim.WrongTargetPenalty
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.theme.MeowfiaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SimulationScreen(
    onBack: () -> Unit
) {
    var playerCount by remember { mutableIntStateOf(6) }
    var roundCount by remember { mutableIntStateOf(3) }
    var gameCount by remember { mutableIntStateOf(1) }
    var includeFlowers by remember { mutableStateOf(false) }
    var strategyDist by remember { mutableStateOf(StrategyDistribution.BALANCED) }
    var showAdvanced by remember { mutableStateOf(false) }
    var seedText by remember { mutableStateOf("") }
    var meowfiaChance by remember { mutableFloatStateOf(0.33f) }
    val defaultAllowed = remember {
        RoleId.entries.filter { it.implemented && !it.isFlower && !it.isBuffer }.toSet()
    }
    var allowedRoles by remember { mutableStateOf(defaultAllowed) }

    // Scoring rules state
    var startingHandSize by remember { mutableIntStateOf(3) }
    var postRoundDraw by remember { mutableIntStateOf(2) }
    var handCap by remember { mutableIntStateOf(0) }
    var finalCardValue by remember { mutableIntStateOf(1) }
    var minThrowPerRound by remember { mutableIntStateOf(1) }
    var winThrownAction by remember { mutableStateOf(WinThrownAction.BANK_TO_SCORE_PILE) }
    var flatWinBonusPts by remember { mutableIntStateOf(0) }
    var lossThrownAction by remember { mutableStateOf(LossThrownAction.DISCARD) }
    var flatLossPenaltyPts by remember { mutableIntStateOf(0) }
    var consolation by remember { mutableStateOf(Consolation.RETURN_HIGHEST_TO_HAND) }
    var wrongTargetPenalty by remember { mutableStateOf(WrongTargetPenalty.NONE) }
    var tieBreaker by remember { mutableStateOf(TieBreaker.MEOWFIA_WINS) }

    var singleResult by remember { mutableStateOf<SimGameResult?>(null) }
    var batchResult by remember { mutableStateOf<BatchStatistics?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }

    // Background execution
    var runTrigger by remember { mutableIntStateOf(0) }

    if (runTrigger > 0) {
        LaunchedEffect(runTrigger) {
            isRunning = true
            withContext(Dispatchers.Default) {
                RoleRegistry.initialize()
                FlowerRegistry.initialize()

                val seed = seedText.toLongOrNull() ?: System.currentTimeMillis()
                val names = SimConfig.DEFAULT_NAMES.shuffled().take(playerCount)
                val scoringRules = ScoringRules(
                    startingHandSize = startingHandSize,
                    postRoundDrawCount = postRoundDraw,
                    handCap = handCap,
                    finalCardValue = finalCardValue,
                    minThrowPerRound = minThrowPerRound,
                    winThrownAction = winThrownAction,
                    flatWinPoints = flatWinBonusPts,
                    lossThrownAction = lossThrownAction,
                    flatLossPenaltyPoints = flatLossPenaltyPts,
                    consolation = consolation,
                    wrongTargetPenalty = wrongTargetPenalty,
                    tieBreaker = tieBreaker
                )
                val config = SimConfig(
                    seed = seed,
                    playerCount = playerCount,
                    playerNames = names,
                    roundCount = roundCount,
                    includeFlowers = includeFlowers,
                    strategyDistribution = strategyDist,
                    meowfiaChance = if (meowfiaChance != 0.33f) meowfiaChance else null,
                    verbosity = if (gameCount == 1) Verbosity.FULL else Verbosity.MINIMAL,
                    allowedRoles = allowedRoles.ifEmpty { null },
                    scoringRules = scoringRules
                )

                if (gameCount == 1) {
                    progress = "Running simulation..."
                    val result = SimEngine(config).runGame()
                    singleResult = result
                    batchResult = null
                } else {
                    singleResult = null
                    val stats = BatchRunner.run(
                        gameCount = gameCount,
                        baseConfig = config,
                        collectSamples = 1
                    ) { completed, total ->
                        progress = "Running game $completed / $total..."
                    }
                    batchResult = stats
                }
            }
            isRunning = false
            progress = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Simulation Mode",
            color = MeowfiaColors.Primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (singleResult == null && batchResult == null) {
            // Config UI
            LazyColumn(modifier = Modifier.weight(1f)) {
                item { ConfigSection(
                    playerCount = playerCount,
                    roundCount = roundCount,
                    gameCount = gameCount,
                    includeFlowers = includeFlowers,
                    strategyDist = strategyDist,
                    showAdvanced = showAdvanced,
                    seedText = seedText,
                    meowfiaChance = meowfiaChance,
                    allowedRoles = allowedRoles,
                    defaultAllowed = defaultAllowed,
                    startingHandSize = startingHandSize,
                    postRoundDraw = postRoundDraw,
                    handCap = handCap,
                    finalCardValue = finalCardValue,
                    minThrowPerRound = minThrowPerRound,
                    winThrownAction = winThrownAction,
                    flatWinBonusPts = flatWinBonusPts,
                    lossThrownAction = lossThrownAction,
                    flatLossPenaltyPts = flatLossPenaltyPts,
                    consolation = consolation,
                    wrongTargetPenalty = wrongTargetPenalty,
                    tieBreaker = tieBreaker,
                    onPlayerCountChange = { playerCount = it },
                    onRoundCountChange = { roundCount = it },
                    onGameCountChange = { gameCount = it },
                    onFlowersChange = { includeFlowers = it },
                    onStrategyChange = { strategyDist = it },
                    onToggleAdvanced = { showAdvanced = !showAdvanced },
                    onSeedChange = { seedText = it },
                    onMeowfiaChanceChange = { meowfiaChance = it },
                    onAllowedRolesChange = { allowedRoles = it },
                    onStartingHandSizeChange = { startingHandSize = it },
                    onPostRoundDrawChange = { postRoundDraw = it },
                    onHandCapChange = { handCap = it },
                    onFinalCardValueChange = { finalCardValue = it },
                    onMinThrowChange = { minThrowPerRound = it },
                    onWinThrownActionChange = { winThrownAction = it },
                    onFlatWinBonusPtsChange = { flatWinBonusPts = it },
                    onLossThrownActionChange = { lossThrownAction = it },
                    onFlatLossPenaltyPtsChange = { flatLossPenaltyPts = it },
                    onConsolationChange = { consolation = it },
                    onWrongTargetPenaltyChange = { wrongTargetPenalty = it },
                    onTieBreakerChange = { tieBreaker = it }
                ) }
            }

            if (isRunning) {
                Text(
                    text = progress,
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.align(ComposeAlignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MeowfiaColors.Primary,
                    trackColor = MeowfiaColors.Surface
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            MeowfiaPrimaryButton(
                text = if (gameCount == 1) "Run Simulation" else "Run $gameCount Games",
                enabled = !isRunning,
                onClick = { runTrigger++ }
            )
            Spacer(modifier = Modifier.height(8.dp))
            MeowfiaSecondaryButton(text = "Back", onClick = onBack)
        } else if (singleResult != null) {
            SingleGameResultsView(result = singleResult!!, onBack = {
                singleResult = null
            })
        } else if (batchResult != null) {
            BatchResultsDashboard(stats = batchResult!!, onBack = {
                batchResult = null
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Configuration Section ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigSection(
    playerCount: Int,
    roundCount: Int,
    gameCount: Int,
    includeFlowers: Boolean,
    strategyDist: StrategyDistribution,
    showAdvanced: Boolean,
    seedText: String,
    meowfiaChance: Float,
    allowedRoles: Set<RoleId>,
    defaultAllowed: Set<RoleId>,
    startingHandSize: Int,
    postRoundDraw: Int,
    handCap: Int,
    finalCardValue: Int,
    minThrowPerRound: Int,
    winThrownAction: WinThrownAction,
    flatWinBonusPts: Int,
    lossThrownAction: LossThrownAction,
    flatLossPenaltyPts: Int,
    consolation: Consolation,
    wrongTargetPenalty: WrongTargetPenalty,
    tieBreaker: TieBreaker,
    onPlayerCountChange: (Int) -> Unit,
    onRoundCountChange: (Int) -> Unit,
    onGameCountChange: (Int) -> Unit,
    onFlowersChange: (Boolean) -> Unit,
    onStrategyChange: (StrategyDistribution) -> Unit,
    onToggleAdvanced: () -> Unit,
    onSeedChange: (String) -> Unit,
    onMeowfiaChanceChange: (Float) -> Unit,
    onAllowedRolesChange: (Set<RoleId>) -> Unit,
    onStartingHandSizeChange: (Int) -> Unit,
    onPostRoundDrawChange: (Int) -> Unit,
    onHandCapChange: (Int) -> Unit,
    onFinalCardValueChange: (Int) -> Unit,
    onMinThrowChange: (Int) -> Unit,
    onWinThrownActionChange: (WinThrownAction) -> Unit,
    onFlatWinBonusPtsChange: (Int) -> Unit,
    onLossThrownActionChange: (LossThrownAction) -> Unit,
    onFlatLossPenaltyPtsChange: (Int) -> Unit,
    onConsolationChange: (Consolation) -> Unit,
    onWrongTargetPenaltyChange: (WrongTargetPenalty) -> Unit,
    onTieBreakerChange: (TieBreaker) -> Unit
) {
    Column {
        SectionHeader("Basic Settings")
        SimConfigRow("Players", playerCount, 3, 8,
            onMinus = { onPlayerCountChange(playerCount - 1) },
            onPlus = { onPlayerCountChange(playerCount + 1) })
        Spacer(modifier = Modifier.height(8.dp))
        SimConfigRow("Rounds", roundCount, 1, 10,
            onMinus = { onRoundCountChange(roundCount - 1) },
            onPlus = { onRoundCountChange(roundCount + 1) })
        Spacer(modifier = Modifier.height(8.dp))
        SimConfigRow("Games", gameCount, 1, 1000,
            onMinus = { onGameCountChange(stepDown(gameCount)) },
            onPlus = { onGameCountChange(stepUp(gameCount)) })

        Spacer(modifier = Modifier.height(16.dp))

        // Flowers toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = ComposeAlignment.CenterVertically
        ) {
            Text("Include Flowers", color = MeowfiaColors.TextPrimary, fontSize = 18.sp)
            Switch(
                checked = includeFlowers,
                onCheckedChange = onFlowersChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MeowfiaColors.Primary,
                    checkedTrackColor = MeowfiaColors.Primary.copy(alpha = 0.4f),
                    uncheckedThumbColor = MeowfiaColors.TextSecondary,
                    uncheckedTrackColor = MeowfiaColors.Surface
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionHeader("Strategy Distribution")
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StrategyDistribution.entries.forEach { dist ->
                FilterChip(
                    selected = strategyDist == dist,
                    onClick = { onStrategyChange(dist) },
                    label = { Text(dist.displayName, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeowfiaColors.Primary.copy(alpha = 0.3f),
                        selectedLabelColor = MeowfiaColors.Primary,
                        containerColor = MeowfiaColors.Surface,
                        labelColor = MeowfiaColors.TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Advanced toggle
        Text(
            text = if (showAdvanced) "▼ Advanced Settings" else "▶ Advanced Settings",
            color = MeowfiaColors.Primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onToggleAdvanced() }
        )

        AnimatedVisibility(visible = showAdvanced) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))

                // Seed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = ComposeAlignment.CenterVertically
                ) {
                    Text("Seed", color = MeowfiaColors.TextPrimary, fontSize = 16.sp)
                    androidx.compose.material3.OutlinedTextField(
                        value = seedText,
                        onValueChange = { onSeedChange(it.filter { c -> c.isDigit() }) },
                        modifier = Modifier.width(160.dp),
                        placeholder = { Text("Auto", color = MeowfiaColors.TextSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MeowfiaColors.TextPrimary,
                            unfocusedTextColor = MeowfiaColors.TextPrimary,
                            focusedBorderColor = MeowfiaColors.Primary,
                            unfocusedBorderColor = MeowfiaColors.TextSecondary.copy(alpha = 0.3f),
                            cursorColor = MeowfiaColors.Primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Meowfia chance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = ComposeAlignment.CenterVertically
                ) {
                    Text("Meowfia %", color = MeowfiaColors.TextPrimary, fontSize = 16.sp)
                    Text(
                        "${"%.0f".format(meowfiaChance * 100)}%",
                        color = MeowfiaColors.Primary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                androidx.compose.material3.Slider(
                    value = meowfiaChance,
                    onValueChange = onMeowfiaChanceChange,
                    valueRange = 0.10f..0.60f,
                    steps = 9,
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = MeowfiaColors.Primary,
                        activeTrackColor = MeowfiaColors.Primary,
                        inactiveTrackColor = MeowfiaColors.Surface
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Role Pool Selector
        SectionHeader("Role Pool")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = ComposeAlignment.CenterVertically
        ) {
            Text(
                "${allowedRoles.size} roles selected",
                color = MeowfiaColors.TextSecondary,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "All",
                    color = MeowfiaColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        val allToggleable = RoleId.entries.filter { it.implemented && !it.isFlower }
                        onAllowedRolesChange(allToggleable.toSet())
                    }
                )
                Text(
                    "Clear",
                    color = MeowfiaColors.Secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onAllowedRolesChange(emptySet()) }
                )
                Text(
                    "Default",
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onAllowedRolesChange(defaultAllowed) }
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val toggleableRoles = RoleId.entries.filter { it.implemented && !it.isFlower }
            for (role in toggleableRoles) {
                val selected = role in allowedRoles
                FilterChip(
                    selected = selected,
                    onClick = {
                        onAllowedRolesChange(
                            if (selected) allowedRoles - role else allowedRoles + role
                        )
                    },
                    label = { Text(role.displayName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = if (role.isBuffer) MeowfiaColors.TextSecondary.copy(alpha = 0.3f)
                            else if (role.isMeowfiaAnimal) MeowfiaColors.Meowfia.copy(alpha = 0.3f)
                            else MeowfiaColors.Farm.copy(alpha = 0.3f),
                        selectedLabelColor = if (role.isBuffer) MeowfiaColors.TextPrimary
                            else if (role.isMeowfiaAnimal) MeowfiaColors.Meowfia
                            else MeowfiaColors.Farm,
                        containerColor = MeowfiaColors.Surface,
                        labelColor = MeowfiaColors.TextSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ScoringSettingsSection(
            startingHandSize = startingHandSize,
            postRoundDraw = postRoundDraw,
            handCap = handCap,
            finalCardValue = finalCardValue,
            minThrowPerRound = minThrowPerRound,
            winThrownAction = winThrownAction,
            flatWinBonusPts = flatWinBonusPts,
            lossThrownAction = lossThrownAction,
            flatLossPenaltyPts = flatLossPenaltyPts,
            consolation = consolation,
            wrongTargetPenalty = wrongTargetPenalty,
            tieBreaker = tieBreaker,
            onStartingHandSizeChange = onStartingHandSizeChange,
            onPostRoundDrawChange = onPostRoundDrawChange,
            onHandCapChange = onHandCapChange,
            onFinalCardValueChange = onFinalCardValueChange,
            onMinThrowChange = onMinThrowChange,
            onWinThrownActionChange = onWinThrownActionChange,
            onFlatWinBonusPtsChange = onFlatWinBonusPtsChange,
            onLossThrownActionChange = onLossThrownActionChange,
            onFlatLossPenaltyPtsChange = onFlatLossPenaltyPtsChange,
            onConsolationChange = onConsolationChange,
            onWrongTargetPenaltyChange = onWrongTargetPenaltyChange,
            onTieBreakerChange = onTieBreakerChange
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ── Single Game Results ──

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SingleGameResultsView(
    result: SimGameResult,
    onBack: () -> Unit
) {
    val displayNames = result.finalScores.indices.map { i ->
        val name = result.playerNames.getOrElse(i) { "P$i" }
        val strat = result.strategies.getOrElse(i) { "" }
        "$name ($strat)"
    }
    val scores = result.finalScores.mapIndexed { i, s -> displayNames[i] to s }
    val winner = scores.maxByOrNull { it.second }

    // Everything in one scrollable list
    LazyColumn(modifier = Modifier.weight(1f)) {
        item {
            Text(
                text = "Game Complete — Seed: ${result.seed}",
                color = MeowfiaColors.TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Final scores card
        item {
            SummaryCard("Final Scores") {
                scores.sortedByDescending { it.second }.forEach { (strategy, score) ->
                    val isWinner = score == winner?.second
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = strategy,
                            color = if (isWinner) MeowfiaColors.Primary else MeowfiaColors.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$score pts${if (isWinner) " ★" else ""}",
                            color = if (isWinner) MeowfiaColors.Primary else MeowfiaColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Score progression
        if (result.perRoundDeltas.isNotEmpty() && result.roundLogs.size > 1) {
            item {
                SummaryCard("Score Progression") {
                    for ((roundIdx, log) in result.roundLogs.withIndex()) {
                        val scoresAtRound = log.postScores
                        val maxScore = scoresAtRound.maxOrNull()?.coerceAtLeast(1) ?: 1
                        Text(
                            "Round ${roundIdx + 1}:",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 12.sp
                        )
                        scoresAtRound.forEachIndexed { i, s ->
                            val name = result.playerNames.getOrElse(i) { "P$i" }.take(8)
                            val barLen = ((s.toFloat() / maxScore) * 20).toInt().coerceIn(0, 20)
                            val bar = "█".repeat(barLen)
                            Text(
                                "  $name ${s.toString().padStart(4)} $bar",
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Solvability overview
        val solvResults = result.roundLogs.mapNotNull { it.solvability }
        if (solvResults.isNotEmpty()) {
            item {
                val avgSolv = solvResults
                    .filter { it.totalCandidates > 0 }
                    .map { (1.0 - it.consistentWorlds.toDouble() / it.totalCandidates) * 100 }
                    .let { if (it.isNotEmpty()) it.average() else 0.0 }
                SummaryCard("Solvability") {
                    StatRow("Avg Solvability", "%.1f%%".format(avgSolv))
                    Spacer(modifier = Modifier.height(4.dp))
                    for ((idx, log) in result.roundLogs.withIndex()) {
                        val s = log.solvability ?: continue
                        val pct = if (s.totalCandidates > 0) {
                            ((1.0 - s.consistentWorlds.toDouble() / s.totalCandidates) * 100)
                        } else 100.0
                        val verdict = s.verdictLabel
                        val vColor = when (s.solvability) {
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.SOLVED -> MeowfiaColors.Farm
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.ACTIONABLE -> MeowfiaColors.Farm
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.NARROWED -> MeowfiaColors.Primary
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.COIN_FLIP -> MeowfiaColors.TextSecondary
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("R${idx + 1}: ${"%.0f".format(pct)}%", color = MeowfiaColors.TextPrimary, fontSize = 12.sp)
                            Text(verdict, color = vColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Round-by-round details
        items(result.roundLogs, key = { it.roundNum }) { roundLog ->
            RoundDetailCard(roundLog = roundLog, strategies = displayNames)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    MeowfiaSecondaryButton(text = "Run Another", onClick = onBack)
}

// ── Batch Results Dashboard ──

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.BatchResultsDashboard(
    stats: BatchStatistics,
    onBack: () -> Unit
) {
    // Pre-compute all formatted/sorted data once
    val sortedStrategies = remember(stats) {
        val bestMean = stats.strategyMeans.values.maxOrNull() ?: 0.0
        stats.strategyMeans.entries.sortedByDescending { it.value }.map { (name, mean) ->
            val std = stats.strategyStdDevs[name] ?: 0.0
            val diff = mean - bestMean
            StrategyRow(name, mean, std, diff, diff == 0.0)
        }
    }
    val sortedRoles = remember(stats) {
        stats.roleMetrics.entries.sortedByDescending { it.value.teamWinRate }.map { (id, s) ->
            RoleRow(id.displayName, s.timesAssigned, s.avgEggDelta, s.teamWinRate)
        }
    }
    val sortedMeowfiaCounts = remember(stats) { stats.meowfiaCountDistribution.keys.sorted() }
    val sortedWinLoss = remember(stats) { stats.winLossPatterns.values.sortedByDescending { it.winnerCount }.take(10) }
    val sortedAlignment = remember(stats) { stats.alignmentPatterns.values.sortedByDescending { it.winnerCount }.take(10) }
    val solvBuckets = remember(stats) {
        val b = IntArray(10)
        for (pct in stats.solvabilityPercentages) b[(pct / 10).coerceIn(0, 9)]++
        b.toList()
    }

    LazyColumn(modifier = Modifier.weight(1f)) {
        // Overview
        item(key = "overview") {
            SummaryCard("Overview") {
                StatRow("Games", "${stats.nGames}")
                StatRow("Players", "${stats.nPlayers}")
                StatRow("Rounds/game", "${stats.nRounds}")
                StatRow("Avg Score", "%.1f".format(stats.avgScore))
                StatRow("Median", "%.0f".format(stats.medianScore))
                StatRow("Min / Max", "${stats.minScore} / ${stats.maxScore}")
                StatRow("Std Dev", "%.1f".format(stats.scoreStdDev))
                StatRow("Negative %", "%.1f%%".format(stats.negativeScorePct))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Score Percentiles
        item {
            SummaryCard("Score Percentiles") {
                StatRow("P10", "%.0f".format(stats.scorePercentiles.p10))
                StatRow("P25", "%.0f".format(stats.scorePercentiles.p25))
                StatRow("Median", "%.0f".format(stats.medianScore))
                StatRow("P75", "%.0f".format(stats.scorePercentiles.p75))
                StatRow("P90", "%.0f".format(stats.scorePercentiles.p90))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Balance Health
        item {
            SummaryCard("Balance Health") {
                HealthRow("Score Equality (Gini)", "%.3f".format(stats.avgGini), stats.avgGini < 0.35)
                HealthRow("Skill Premium", "%+.1f".format(stats.skillPremium), stats.skillPremium in 5.0..30.0)
                HealthRow("Aggro-Cons Gap", "%+.1f".format(stats.aggroConsGap), stats.aggroConsGap in -10.0..10.0)
                HealthRow("Catch-up Rate", "%.3f".format(stats.catchUpRate), stats.catchUpRate >= 0.3)
                HealthRow("Deduction Corr.", "%.3f".format(stats.deductionCorrelation), stats.deductionCorrelation >= 0.5)
                HealthRow("Strategy Viability", "%.0f%%".format(stats.strategyViability * 100), stats.strategyViability >= 0.66)
                HealthRow("Comeback Freq.", "%.1f%%".format(stats.comebackFrequency * 100), stats.comebackFrequency >= 0.2)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Strategy Performance
        item(key = "strategy_perf") {
            SummaryCard("Strategy Performance") {
                // Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Archetype", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Avg", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    Text("SD", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    Text("vs Best", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                }
                Spacer(modifier = Modifier.height(4.dp))
                for (row in sortedStrategies) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(
                            row.name,
                            color = if (row.isBest) MeowfiaColors.Primary else MeowfiaColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (row.isBest) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text("%.1f".format(row.mean), color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                        Text("%.1f".format(row.std), color = MeowfiaColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                        Text(
                            "%+.1f".format(row.diff),
                            color = if (row.isBest) MeowfiaColors.Primary else MeowfiaColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.width(52.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Alignment & Elimination
        item {
            SummaryCard("Alignment & Elimination") {
                HealthRow("Farm Win Rate", "%.1f%%".format(stats.farmWinRate * 100), stats.farmWinRate in 0.35..0.65)
                StatRow("Zero-Meowfia Rounds", "%.1f%%".format(stats.zeroMeowfiaRate * 100))
                StatRow("All-Meowfia Rounds", "%.1f%%".format(stats.allMeowfiaRate * 100))
                HealthRow("Elimination Accuracy", "%.1f%%".format(stats.eliminationAccuracy * 100), stats.eliminationAccuracy >= 0.3)
                StatRow("Avg Votes/Elim", "%.1f".format(stats.avgVotesPerElimination))
                StatRow("Unanimous Votes", "%.1f%%".format(stats.unanimousVoteRate * 100))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Role Distribution
        if (stats.roleMetrics.isNotEmpty()) {
            item {
                SummaryCard("Role Performance") {
                    // Header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Role", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("Used", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                        Text("Avg Egg", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                        Text("Win%", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    for (row in sortedRoles) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(row.name, color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("${row.assigned}", color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text(
                                "%+.1f".format(row.avgEgg),
                                color = if (row.avgEgg > 0) MeowfiaColors.Farm else if (row.avgEgg < 0) MeowfiaColors.Meowfia else MeowfiaColors.TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(52.dp),
                                textAlign = TextAlign.End
                            )
                            Text("%.0f".format(row.winRate * 100), color = MeowfiaColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Deducibility
        item {
            SummaryCard("Deducibility") {
                HealthRow("Solved", "%.1f%%".format(stats.solvedRate * 100), stats.solvedRate < 0.5)
                StatRow("Actionable", "%.1f%%".format(stats.actionableRate * 100))
                StatRow("Narrowed", "%.1f%%".format(stats.narrowedRate * 100))
                if (stats.narrowedBySuspectCount.isNotEmpty()) {
                    for ((count, rate) in stats.narrowedBySuspectCount.toSortedMap()) {
                        val label = when (count) {
                            0 -> "  → 0 suspects"
                            1 -> "  → 1 suspect"
                            else -> "  → $count suspects"
                        }
                        StatRow(label, "%.1f%%".format(rate * 100))
                    }
                }
                StatRow("Could Be Anyone", "%.1f%%".format(stats.coinFlipRate * 100))
                if (stats.avgSuspectsWhenNarrowed > 0) {
                    StatRow("Avg Suspects (narrowed)", "%.1f".format(stats.avgSuspectsWhenNarrowed))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Farm vs Meowfia Wins
        item {
            SummaryCard("Farm vs Meowfia Wins") {
                val farmPct = (stats.farmWinRate * 100).toInt()
                val meowfiaPct = 100 - farmPct
                val barWidth = 24
                val farmBar = barWidth * farmPct / 100
                val meowfiaBar = barWidth - farmBar
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("Farm    ", color = MeowfiaColors.TextSecondary, fontSize = 12.sp)
                    Text(
                        "${"█".repeat(farmBar)}${"░".repeat(meowfiaBar)}",
                        color = MeowfiaColors.Farm,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text("  $farmPct%", color = MeowfiaColors.Farm, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("Meowfia ", color = MeowfiaColors.TextSecondary, fontSize = 12.sp)
                    Text(
                        "${"░".repeat(farmBar)}${"█".repeat(meowfiaBar)}",
                        color = MeowfiaColors.Meowfia,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text("  $meowfiaPct%", color = MeowfiaColors.Meowfia, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Meowfia Count Distribution
        if (stats.meowfiaCountDistribution.isNotEmpty()) {
            item {
                SummaryCard("Meowfia Count Distribution") {
                    val totalRounds = stats.meowfiaCountDistribution.values.sum().coerceAtLeast(1)
                    val maxCount = stats.meowfiaCountDistribution.values.maxOrNull() ?: 1
                    for (mc in stats.meowfiaCountDistribution.keys.sorted()) {
                        val count = stats.meowfiaCountDistribution[mc] ?: 0
                        val pct = count * 100 / totalRounds
                        val barLen = (count * 16 / maxCount).coerceIn(0, 16)
                        val winRate = stats.meowfiaCountWinRates[mc]
                        val winStr = if (mc == 0) "N/A" else "Farm wins ${"%.0f".format((winRate ?: 0.0) * 100)}%"
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(
                                "${mc}M",
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                "█".repeat(barLen),
                                color = MeowfiaColors.Meowfia,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                " $pct% ($winStr)",
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Role Win Rate Leaderboard
        if (stats.roleWinRates.size >= 2) {
            item {
                SummaryCard("Role Win Rate Leaderboard") {
                    val sorted = stats.roleWinRates.entries.sortedByDescending { it.value }
                    val top5 = sorted.take(5)
                    val bottom5 = sorted.takeLast(5).reversed()
                    Text("Top 5", color = MeowfiaColors.Farm, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    for ((role, rate) in top5) {
                        val pct = (rate * 100).toInt()
                        val barLen = (pct * 16 / 100).coerceIn(0, 16)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(
                                role.displayName,
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                "█".repeat(barLen),
                                color = MeowfiaColors.Farm,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(" $pct%", color = MeowfiaColors.TextPrimary, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Bottom 5", color = MeowfiaColors.Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(2.dp))
                    for ((role, rate) in bottom5) {
                        val pct = (rate * 100).toInt()
                        val barLen = (pct * 16 / 100).coerceIn(0, 16)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(
                                role.displayName,
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(100.dp)
                            )
                            Text(
                                "█".repeat(barLen),
                                color = MeowfiaColors.Secondary,
                                fontSize = 12.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(" $pct%", color = MeowfiaColors.TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Solvability Distribution
        if (stats.solvabilityPercentages.isNotEmpty()) {
            item {
                SummaryCard("Solvability Distribution") {
                    StatRow("Avg Solvability", "%.1f%%".format(stats.avgSolvabilityPercent))
                    Spacer(modifier = Modifier.height(4.dp))
                    val maxBucket = solvBuckets.maxOrNull()?.coerceAtLeast(1) ?: 1
                    for (i in 0 until 10) {
                        val lo = i * 10
                        val hi = lo + 10
                        val barLen = (solvBuckets[i] * 20 / maxBucket).coerceIn(0, 20)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(
                                "${lo}-${hi}%",
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                "█".repeat(barLen),
                                color = MeowfiaColors.Primary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                " ${solvBuckets[i]}",
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Night Economy
        item {
            SummaryCard("Night Economy") {
                StatRow("Eggs Created/Round", "%.1f".format(stats.nightMetrics.avgEggsCreatedPerRound))
                StatRow("Eggs Stolen/Round", "%.1f".format(stats.nightMetrics.avgEggsStolenPerRound))
                StatRow("Net Eggs/Round", "%.1f".format(stats.nightMetrics.avgNetEggsPerRound))
                StatRow("Zero Delta Rate", "%.1f%%".format(stats.nightMetrics.zeroEggDeltaRate * 100))
                StatRow("Visits/Player", "%.2f".format(stats.nightMetrics.avgVisitsPerPlayer))
                StatRow("Info Lines/Player", "%.1f".format(stats.nightMetrics.avgInfoLinesPerPlayer))
                StatRow("Confused/Round", "%.2f".format(stats.nightMetrics.avgConfusedPerRound))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Per-Round Progression
        if (stats.perRoundAvgScores.isNotEmpty()) {
            item {
                SummaryCard("Score Progression (Avg)") {
                    val maxAvg = stats.perRoundAvgScores.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    for ((idx, avg) in stats.perRoundAvgScores.withIndex()) {
                        val barLen = ((avg / maxAvg) * 20).toInt().coerceIn(0, 20)
                        val bar = "█".repeat(barLen)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "R${idx + 1}",
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(28.dp)
                            )
                            Text(
                                "%.1f".format(avg),
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.End
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(bar, color = MeowfiaColors.Primary, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Win/Loss Patterns
        if (stats.winLossPatterns.isNotEmpty()) {
            item {
                PatternTable("Win/Loss Patterns", stats.winLossPatterns)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Alignment Patterns
        if (stats.alignmentPatterns.isNotEmpty()) {
            item {
                PatternTable("Farm/Meowfia Patterns", stats.alignmentPatterns)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Hand Luck Correlation
        item {
            SummaryCard("Hand Luck vs Score") {
                val corrStr = "%.3f".format(stats.handLuckCorrelation)
                val verdict = when {
                    stats.handLuckCorrelation > 0.3 -> "Strong — lucky hands win more"
                    stats.handLuckCorrelation > 0.1 -> "Moderate — some luck advantage"
                    stats.handLuckCorrelation > -0.1 -> "Negligible — skill matters more than card luck"
                    else -> "Inverse — high cards don't help"
                }
                StatRow("Correlation", corrStr)
                Text(verdict, color = MeowfiaColors.TextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                // Luck buckets bar chart
                val maxScore = stats.handLuckBuckets.maxOfOrNull { it.avgFinalScore }?.coerceAtLeast(1.0) ?: 1.0
                for (bucket in stats.handLuckBuckets) {
                    if (bucket.count == 0) continue
                    val barLen = ((bucket.avgFinalScore / maxScore) * 15).toInt().coerceIn(0, 15)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = ComposeAlignment.CenterVertically) {
                        Text(bucket.label, color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(72.dp))
                        Text("%.1f avg".format(bucket.avgFinalScore), color = MeowfiaColors.TextPrimary, fontSize = 11.sp, modifier = Modifier.width(56.dp))
                        Text("\u2588".repeat(barLen), color = MeowfiaColors.Primary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Record Facts
        item {
            val rf = stats.recordFacts
            SummaryCard("Records") {
                rf.highestFinalScore?.let {
                    RecordRow("Highest final score", "${it.playerName}: ${it.value} pts")
                }
                rf.lowestFinalScore?.let {
                    RecordRow("Lowest final score", "${it.playerName}: ${it.value} pts")
                }
                rf.mostPointsSingleRound?.let {
                    RecordRow("Most points in a round", "${it.playerName}: +${it.value} pts (R${it.roundNum})")
                }
                rf.biggestLoss?.let {
                    RecordRow("Biggest loss in a round", "${it.playerName}: ${it.value} pts (R${it.roundNum})")
                }
                rf.mostEggsGained?.let {
                    RecordRow("Most eggs gained", "${it.playerName}: +${it.value} eggs (R${it.roundNum})")
                }
                rf.mostEggsLost?.let {
                    RecordRow("Most eggs lost", "${it.playerName}: ${it.value} eggs (R${it.roundNum})")
                }
                rf.mostCardsBet?.let {
                    RecordRow("Most cards thrown", "${it.playerName}: ${it.value} cards (R${it.roundNum})")
                }
                rf.highestCardsBetAgainstOne?.let {
                    RecordRow("Most cards thrown at one player", "${it.value} cards at ${it.playerName} (R${it.roundNum})")
                }
                rf.biggestCatchUp?.let {
                    RecordRow("Biggest catch-up", "${it.playerName}: +${it.value} pts from mid to end")
                }
                rf.bestRole?.let { RecordRow("Best role (win rate)", it) }
                rf.worstRole?.let { RecordRow("Worst role (win rate)", it) }
                rf.bestArchetype?.let { RecordRow("Best archetype", it) }
                rf.bestWinLossPattern?.let { RecordRow("Highest win rate W/L", it) }
                rf.worstWinLossPattern?.let { RecordRow("Lowest win rate W/L", it) }
                rf.bestAlignmentPattern?.let { RecordRow("Highest win rate F/M", it) }
                rf.worstAlignmentPattern?.let { RecordRow("Lowest win rate F/M", it) }
                rf.mostRoleSwaps?.let {
                    if (it.value > 0) RecordRow("Most role swaps in a round", "${it.value} swaps (R${it.roundNum})")
                }
                rf.mostConfused?.let {
                    if (it.value > 0) RecordRow("Most confused in a round", "${it.value} players (R${it.roundNum})")
                }
                rf.mostHugged?.let {
                    if (it.value > 0) RecordRow("Most hugged in a round", "${it.value} players (R${it.roundNum})")
                }
                rf.mostWinks?.let {
                    if (it.value > 0) RecordRow("Most winks in a round", "${it.value} players (R${it.roundNum})")
                }
                RecordRow("Vote draws (ties)", "${rf.drawCount} rounds")
                if (rf.roundsWithNoBuffers > 0) {
                    RecordRow("Rounds with no default roles", "${rf.roundsWithNoBuffers}")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
    MeowfiaSecondaryButton(text = "Run Another", onClick = onBack)
}

// ── Shared Components ──

@Composable
private fun SummaryCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(1.dp, MeowfiaColors.Primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, color = MeowfiaColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MeowfiaColors.TextSecondary, fontSize = 13.sp)
        Text(value, color = MeowfiaColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HealthRow(label: String, value: String, healthy: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = ComposeAlignment.CenterVertically
    ) {
        Row(verticalAlignment = ComposeAlignment.CenterVertically) {
            Text(
                if (healthy) "✓" else "✗",
                color = if (healthy) MeowfiaColors.Farm else MeowfiaColors.Secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, color = MeowfiaColors.TextSecondary, fontSize = 13.sp)
        }
        Text(
            value,
            color = if (healthy) MeowfiaColors.Farm else MeowfiaColors.Secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = MeowfiaColors.Primary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SimConfigRow(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = ComposeAlignment.CenterVertically
    ) {
        Text(label, color = MeowfiaColors.TextPrimary, fontSize = 18.sp)
        Row(verticalAlignment = ComposeAlignment.CenterVertically) {
            MeowfiaSecondaryButton(
                text = "-",
                onClick = onMinus,
                modifier = Modifier.width(48.dp),
                enabled = value > min
            )
            Text(
                text = "$value",
                color = MeowfiaColors.Primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            MeowfiaSecondaryButton(
                text = "+",
                onClick = onPlus,
                modifier = Modifier.width(48.dp),
                enabled = value < max
            )
        }
    }
}

@Composable
private fun RoundDetailCard(
    roundLog: SimRoundLog,
    strategies: List<String>
) {
    val vr = roundLog.votingResult

    Column(modifier = Modifier.fillMaxWidth()) {
        // Round header
        Text(
            "Round ${roundLog.roundNum}",
            color = MeowfiaColors.Primary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        SummaryCard("Summary") {
            if (vr != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${vr.winningTeam.displayName} wins",
                        color = if (vr.winningTeam == GameAlignment.FARM) MeowfiaColors.Farm else MeowfiaColors.Meowfia,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    roundLog.solvability?.let { solve ->
                        val label = solve.verdictLabel
                        val sColor = when (solve.solvability) {
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.SOLVED -> MeowfiaColors.Farm
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.ACTIONABLE -> MeowfiaColors.Farm
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.NARROWED -> MeowfiaColors.Primary
                            com.meowfia.app.testing.sim.RoundSolver.Solvability.COIN_FLIP -> MeowfiaColors.TextSecondary
                        }
                        Text(label, color = sColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (roundLog.pool.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Pool: ${roundLog.pool.joinToString(", ") { it.displayName }}",
                    color = MeowfiaColors.TextSecondary, fontSize = 12.sp
                )
            }
            Text("Meowfia count: ${roundLog.meowfiaCount}", color = MeowfiaColors.Meowfia, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Assignments
        RoundSubCard("Players") {
            for (a in roundLog.assignments) {
                val name = strategies.getOrElse(a.playerId) { "P${a.playerId}" }
                val alignColor = if (a.alignment == GameAlignment.FARM) MeowfiaColors.Farm else MeowfiaColors.Meowfia
                val dawn = roundLog.dawnReports.find { it.playerId == a.playerId }
                val eggDelta = dawn?.actualEggDelta ?: 0
                val eggStr = when {
                    eggDelta > 0 -> "+$eggDelta"
                    eggDelta < 0 -> "$eggDelta"
                    else -> "0"
                }
                val isEliminated = vr?.eliminatedId == a.playerId

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = ComposeAlignment.CenterVertically
                ) {
                    RoleIcon(roleId = a.roleId, size = 28)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = ComposeAlignment.CenterVertically) {
                            Text(name, color = MeowfiaColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                a.alignment.displayName,
                                color = alignColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isEliminated) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ELIMINATED", color = MeowfiaColors.Secondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            "${a.roleId.displayName} · ${eggStr} eggs",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Voting & Betting
        if (vr != null) {
            Spacer(modifier = Modifier.height(8.dp))
            RoundSubCard("Voting & Betting") {
                val eliminatedName = strategies.getOrElse(vr.eliminatedId) { "P${vr.eliminatedId}" }
                Text(
                    "Eliminated: $eliminatedName (${vr.eliminatedAlignment.displayName})",
                    color = MeowfiaColors.Secondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                for (a in roundLog.assignments) {
                    val name = strategies.getOrElse(a.playerId) { "P${a.playerId}" }
                    val thrown = vr.thrown[a.playerId] ?: emptyList()
                    val kept = vr.kept[a.playerId] ?: emptyList()
                    val targetId = vr.targets[a.playerId]
                    val targetName = targetId?.let { strategies.getOrElse(it) { "P$it" } } ?: "?"
                    val isWinner = a.alignment == vr.winningTeam
                    val resultColor = if (isWinner) MeowfiaColors.Farm else MeowfiaColors.Secondary
                    val resultTag = if (isWinner) "WON" else "LOST"

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
                        border = BorderStroke(1.dp, resultColor.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = ComposeAlignment.CenterVertically
                            ) {
                                Text(name, color = MeowfiaColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(resultTag, color = resultColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Target: $targetName", color = MeowfiaColors.TextSecondary, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Threw ${thrown.size} cards", color = MeowfiaColors.TextSecondary, fontSize = 11.sp)
                                    if (thrown.isNotEmpty()) {
                                        Text(
                                            thrown.joinToString(" ") { it.display },
                                            color = MeowfiaColors.TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Text("Value: ${thrown.sumOf { it.value }}", color = MeowfiaColors.TextSecondary, fontSize = 10.sp)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Kept ${kept.size} cards", color = MeowfiaColors.TextSecondary, fontSize = 11.sp)
                                    if (kept.isNotEmpty()) {
                                        Text(
                                            kept.joinToString(" ") { it.display },
                                            color = MeowfiaColors.TextPrimary,
                                            fontSize = 11.sp
                                        )
                                        Text("Value: ${kept.sumOf { it.value }}", color = MeowfiaColors.TextSecondary, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Score changes
        if (roundLog.postScores.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            RoundSubCard("Scores") {
                for (i in roundLog.postScores.indices) {
                    val name = strategies.getOrElse(i) { "P$i" }
                    val postScore = roundLog.postScores[i]
                    val preScore = roundLog.preScores.getOrElse(i) { 0 }
                    val delta = postScore - preScore
                    val deltaStr = if (delta >= 0) "+$delta" else "$delta"
                    val deltaColor = when {
                        delta > 0 -> MeowfiaColors.Farm
                        delta < 0 -> MeowfiaColors.Secondary
                        else -> MeowfiaColors.TextSecondary
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        verticalAlignment = ComposeAlignment.CenterVertically
                    ) {
                        Text(name, color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(
                            "$preScore",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                        Text(" → ", color = MeowfiaColors.TextSecondary, fontSize = 12.sp)
                        Text(
                            "$postScore",
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End
                        )
                        Text(
                            deltaStr,
                            color = deltaColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundSubCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.Surface),
        border = BorderStroke(1.dp, MeowfiaColors.TextSecondary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = MeowfiaColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            content()
        }
    }
}

// ── Scoring Settings ──

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScoringSettingsSection(
    startingHandSize: Int,
    postRoundDraw: Int,
    handCap: Int,
    finalCardValue: Int,
    minThrowPerRound: Int,
    winThrownAction: WinThrownAction,
    flatWinBonusPts: Int,
    lossThrownAction: LossThrownAction,
    flatLossPenaltyPts: Int,
    consolation: Consolation,
    wrongTargetPenalty: WrongTargetPenalty,
    tieBreaker: TieBreaker,
    onStartingHandSizeChange: (Int) -> Unit,
    onPostRoundDrawChange: (Int) -> Unit,
    onHandCapChange: (Int) -> Unit,
    onFinalCardValueChange: (Int) -> Unit,
    onMinThrowChange: (Int) -> Unit,
    onWinThrownActionChange: (WinThrownAction) -> Unit,
    onFlatWinBonusPtsChange: (Int) -> Unit,
    onLossThrownActionChange: (LossThrownAction) -> Unit,
    onFlatLossPenaltyPtsChange: (Int) -> Unit,
    onConsolationChange: (Consolation) -> Unit,
    onWrongTargetPenaltyChange: (WrongTargetPenalty) -> Unit,
    onTieBreakerChange: (TieBreaker) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Text(
        text = if (expanded) "▼ Scoring Rules" else "▶ Scoring Rules",
        color = MeowfiaColors.Primary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable { expanded = !expanded }
    )
    AnimatedVisibility(visible = expanded) {
        Column {
            Spacer(modifier = Modifier.height(8.dp))

            // Hand & Draw
            Text("Hand & Draw", color = MeowfiaColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            SimConfigRow("Starting hand", startingHandSize, 1, 10,
                onMinus = { onStartingHandSizeChange(startingHandSize - 1) },
                onPlus = { onStartingHandSizeChange(startingHandSize + 1) })
            Spacer(modifier = Modifier.height(4.dp))
            SimConfigRow("Post-round draw", postRoundDraw, 0, 5,
                onMinus = { onPostRoundDrawChange(postRoundDraw - 1) },
                onPlus = { onPostRoundDrawChange(postRoundDraw + 1) })
            Spacer(modifier = Modifier.height(4.dp))
            SimConfigRow("Hand cap (0=none)", handCap, 0, 20,
                onMinus = { onHandCapChange(if (handCap == 5) 0 else (handCap - 1).coerceAtLeast(5)) },
                onPlus = { onHandCapChange(if (handCap == 0) 5 else (handCap + 1).coerceAtMost(20)) })
            Spacer(modifier = Modifier.height(4.dp))
            SimConfigRow("Final card value", finalCardValue, -2, 3,
                onMinus = { onFinalCardValueChange(finalCardValue - 1) },
                onPlus = { onFinalCardValueChange(finalCardValue + 1) })
            Spacer(modifier = Modifier.height(4.dp))
            SimConfigRow("Min throw/round", minThrowPerRound, 0, 5,
                onMinus = { onMinThrowChange(minThrowPerRound - 1) },
                onPlus = { onMinThrowChange(minThrowPerRound + 1) })

            Spacer(modifier = Modifier.height(12.dp))

            // Win Rules
            Text("Win Rules", color = MeowfiaColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Win thrown action", color = MeowfiaColors.TextPrimary, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WinThrownAction.entries.forEach { action ->
                    FilterChip(
                        selected = winThrownAction == action,
                        onClick = { onWinThrownActionChange(action) },
                        label = { Text(action.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeowfiaColors.Primary.copy(alpha = 0.3f),
                            selectedLabelColor = MeowfiaColors.Primary,
                            containerColor = MeowfiaColors.Surface,
                            labelColor = MeowfiaColors.TextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            SimConfigRow("Flat win bonus pts", flatWinBonusPts, 0, 5,
                onMinus = { onFlatWinBonusPtsChange(flatWinBonusPts - 1) },
                onPlus = { onFlatWinBonusPtsChange(flatWinBonusPts + 1) })

            Spacer(modifier = Modifier.height(12.dp))

            // Loss Rules
            Text("Loss Rules", color = MeowfiaColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Loss thrown action", color = MeowfiaColors.TextPrimary, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LossThrownAction.entries.forEach { action ->
                    FilterChip(
                        selected = lossThrownAction == action,
                        onClick = { onLossThrownActionChange(action) },
                        label = { Text(action.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeowfiaColors.Secondary.copy(alpha = 0.3f),
                            selectedLabelColor = MeowfiaColors.Secondary,
                            containerColor = MeowfiaColors.Surface,
                            labelColor = MeowfiaColors.TextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            SimConfigRow("Flat loss penalty pts", flatLossPenaltyPts, 0, 5,
                onMinus = { onFlatLossPenaltyPtsChange(flatLossPenaltyPts - 1) },
                onPlus = { onFlatLossPenaltyPtsChange(flatLossPenaltyPts + 1) })

            Spacer(modifier = Modifier.height(12.dp))

            // Consolation & Penalties
            Text("Consolation & Penalties", color = MeowfiaColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Consolation type", color = MeowfiaColors.TextPrimary, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Consolation.entries.forEach { c ->
                    FilterChip(
                        selected = consolation == c,
                        onClick = { onConsolationChange(c) },
                        label = { Text(c.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeowfiaColors.Primary.copy(alpha = 0.3f),
                            selectedLabelColor = MeowfiaColors.Primary,
                            containerColor = MeowfiaColors.Surface,
                            labelColor = MeowfiaColors.TextSecondary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Wrong target penalty", color = MeowfiaColors.TextPrimary, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                WrongTargetPenalty.entries.forEach { p ->
                    FilterChip(
                        selected = wrongTargetPenalty == p,
                        onClick = { onWrongTargetPenaltyChange(p) },
                        label = { Text(p.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeowfiaColors.Secondary.copy(alpha = 0.3f),
                            selectedLabelColor = MeowfiaColors.Secondary,
                            containerColor = MeowfiaColors.Surface,
                            labelColor = MeowfiaColors.TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tie Breaking
            Text("Tie Breaking", color = MeowfiaColors.TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TieBreaker.entries.forEach { tb ->
                    FilterChip(
                        selected = tieBreaker == tb,
                        onClick = { onTieBreakerChange(tb) },
                        label = { Text(tb.displayName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeowfiaColors.Primary.copy(alpha = 0.3f),
                            selectedLabelColor = MeowfiaColors.Primary,
                            containerColor = MeowfiaColors.Surface,
                            labelColor = MeowfiaColors.TextSecondary
                        )
                    )
                }
            }
        }
    }
}

// ── Patterns & Records ──

@Composable
private fun PatternTable(title: String, patterns: Map<String, com.meowfia.app.testing.analysis.WinLossPatternStats>) {
    SummaryCard(title) {
        val sorted = patterns.values
            .sortedByDescending { if (it.occurrences > 0) it.winnerCount.toDouble() / it.occurrences else 0.0 }
            .take(10)
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Pattern", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("Count", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            Text("Avg Pts", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
            Text("Win %", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
        }
        Spacer(modifier = Modifier.height(4.dp))
        for (p in sorted) {
            val winPct = if (p.occurrences > 0) p.winnerCount * 100.0 / p.occurrences else 0.0
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(p.pattern, color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("${p.occurrences}", color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                Text("%.1f".format(p.avgFinalScore), color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                Text("%.0f%%".format(winPct), color = MeowfiaColors.Primary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
        }
    }
}

@Composable
private fun RecordRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, color = MeowfiaColors.TextSecondary, fontSize = 11.sp)
        Text(value, color = MeowfiaColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Smart step for game count: 1,2,3..10,20,30..100,200..1000 */
// Pre-computed display data to avoid recomputation during scroll
private data class StrategyRow(val name: String, val mean: Double, val std: Double, val diff: Double, val isBest: Boolean)
private data class RoleRow(val name: String, val assigned: Int, val avgEgg: Double, val winRate: Double)

private fun stepUp(current: Int): Int = when {
    current < 10 -> current + 1
    current < 100 -> current + 10
    else -> (current + 100).coerceAtMost(1000)
}

private fun stepDown(current: Int): Int = when {
    current <= 10 -> (current - 1).coerceAtLeast(1)
    current <= 100 -> current - 10
    else -> current - 100
}
