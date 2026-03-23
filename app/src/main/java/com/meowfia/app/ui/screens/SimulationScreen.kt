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
import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimEngine
import com.meowfia.app.testing.sim.SimGameResult
import com.meowfia.app.testing.sim.SimRoundLog
import com.meowfia.app.testing.sim.StrategyDistribution
import com.meowfia.app.testing.sim.Verbosity
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
                val config = SimConfig(
                    seed = seed,
                    playerCount = playerCount,
                    roundCount = roundCount,
                    includeFlowers = includeFlowers,
                    strategyDistribution = strategyDist,
                    meowfiaChance = if (meowfiaChance != 0.33f) meowfiaChance else null,
                    verbosity = if (gameCount == 1) Verbosity.FULL else Verbosity.MINIMAL
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
                    onPlayerCountChange = { playerCount = it },
                    onRoundCountChange = { roundCount = it },
                    onGameCountChange = { gameCount = it },
                    onFlowersChange = { includeFlowers = it },
                    onStrategyChange = { strategyDist = it },
                    onToggleAdvanced = { showAdvanced = !showAdvanced },
                    onSeedChange = { seedText = it },
                    onMeowfiaChanceChange = { meowfiaChance = it }
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
    onPlayerCountChange: (Int) -> Unit,
    onRoundCountChange: (Int) -> Unit,
    onGameCountChange: (Int) -> Unit,
    onFlowersChange: (Boolean) -> Unit,
    onStrategyChange: (StrategyDistribution) -> Unit,
    onToggleAdvanced: () -> Unit,
    onSeedChange: (String) -> Unit,
    onMeowfiaChanceChange: (Float) -> Unit
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
    }
}

// ── Single Game Results ──

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.SingleGameResultsView(
    result: SimGameResult,
    onBack: () -> Unit
) {
    val scores = result.finalScores.mapIndexed { i, s -> result.strategies[i] to s }
    val winner = scores.maxByOrNull { it.second }

    Text(
        text = "Game Complete — Seed: ${result.seed}",
        color = MeowfiaColors.TextSecondary,
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Final scores card
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

    // Score progression
    if (result.perRoundDeltas.isNotEmpty() && result.roundLogs.size > 1) {
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
                    val name = result.strategies.getOrElse(i) { "P$i" }.take(8)
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

    // Round-by-round details
    LazyColumn(modifier = Modifier.weight(1f)) {
        items(result.roundLogs, key = { it.roundNum }) { roundLog ->
            RoundDetailCard(roundLog = roundLog, strategies = result.strategies)
            Spacer(modifier = Modifier.height(8.dp))
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
    LazyColumn(modifier = Modifier.weight(1f)) {
        // Overview
        item {
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
        item {
            val bestMean = stats.strategyMeans.values.maxOrNull() ?: 0.0
            SummaryCard("Strategy Performance") {
                // Header
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Archetype", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Avg", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                    Text("SD", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                    Text("vs Best", color = MeowfiaColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(52.dp), textAlign = TextAlign.End)
                }
                Spacer(modifier = Modifier.height(4.dp))
                for ((name, mean) in stats.strategyMeans.entries.sortedByDescending { it.value }) {
                    val std = stats.strategyStdDevs[name] ?: 0.0
                    val diff = mean - bestMean
                    val isBest = diff == 0.0
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text(
                            name,
                            color = if (isBest) MeowfiaColors.Primary else MeowfiaColors.TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        Text("%.1f".format(mean), color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
                        Text("%.1f".format(std), color = MeowfiaColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                        Text(
                            "%+.1f".format(diff),
                            color = if (isBest) MeowfiaColors.Primary else MeowfiaColors.TextSecondary,
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
                    for ((roleId, roleStats) in stats.roleMetrics.entries.sortedByDescending { it.value.timesAssigned }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                            Text(roleId.displayName, color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("${roleStats.timesAssigned}", color = MeowfiaColors.TextPrimary, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                            Text(
                                "%+.1f".format(roleStats.avgEggDelta),
                                color = if (roleStats.avgEggDelta > 0) MeowfiaColors.Farm else if (roleStats.avgEggDelta < 0) MeowfiaColors.Meowfia else MeowfiaColors.TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.width(52.dp),
                                textAlign = TextAlign.End
                            )
                            Text("%.0f".format(roleStats.teamWinRate * 100), color = MeowfiaColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.Surface),
        border = BorderStroke(1.dp, MeowfiaColors.TextSecondary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Round ${roundLog.roundNum}",
                    color = MeowfiaColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (vr != null) {
                    Text(
                        text = "${vr.winningTeam.displayName} wins",
                        color = if (vr.winningTeam == GameAlignment.FARM) MeowfiaColors.Farm else MeowfiaColors.Meowfia,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (roundLog.pool.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text("Pool: ", color = MeowfiaColors.TextSecondary, fontSize = 12.sp)
                    Text(
                        roundLog.pool.joinToString(", ") { it.displayName },
                        color = MeowfiaColors.TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                "Meowfia: ${roundLog.meowfiaCount}",
                color = MeowfiaColors.Meowfia,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 1.dp),
                    verticalAlignment = ComposeAlignment.CenterVertically
                ) {
                    RoleIcon(roleId = a.roleId, size = 20)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = name,
                        color = MeowfiaColors.TextPrimary,
                        fontSize = 13.sp,
                        modifier = Modifier.width(120.dp)
                    )
                    Text(
                        text = a.alignment.displayName.take(1),
                        color = alignColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(16.dp)
                    )
                    Text(
                        text = a.roleId.displayName,
                        color = MeowfiaColors.Primary,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${eggStr}e",
                        color = MeowfiaColors.TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                    if (isEliminated) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("X", color = MeowfiaColors.Secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (vr != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val eliminatedName = strategies.getOrElse(vr.eliminatedId) { "P${vr.eliminatedId}" }
                Text(
                    "Eliminated: $eliminatedName (${vr.eliminatedAlignment.displayName})",
                    color = MeowfiaColors.Secondary,
                    fontSize = 12.sp
                )

                val voteStr = vr.votes.entries
                    .sortedByDescending { it.value }
                    .joinToString("  ") { (pid, votes) ->
                        "${strategies.getOrElse(pid) { "P$pid" }}:$votes"
                    }
                Text("Votes: $voteStr", color = MeowfiaColors.TextSecondary, fontSize = 11.sp)
            }

            if (roundLog.postScores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                val scoreStr = roundLog.postScores.mapIndexed { i, s ->
                    "${strategies.getOrElse(i) { "P$i" }.take(3)}:$s"
                }.joinToString("  ")
                Text("Scores: $scoreStr", color = MeowfiaColors.TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

/** Smart step for game count: 1,2,3..10,20,30..100,200..1000 */
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
