package com.meowfia.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.meowfia.app.testing.sim.SimConfig
import com.meowfia.app.testing.sim.SimEngine
import com.meowfia.app.testing.sim.SimGameResult
import com.meowfia.app.testing.sim.SimRoundLog
import com.meowfia.app.testing.sim.Verbosity
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun SimulationScreen(
    onBack: () -> Unit
) {
    var playerCount by remember { mutableIntStateOf(6) }
    var roundCount by remember { mutableIntStateOf(3) }
    var gameCount by remember { mutableIntStateOf(1) }
    var result by remember { mutableStateOf<SimGameResult?>(null) }
    var isRunning by remember { mutableStateOf(false) }

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

        if (result == null) {
            // Config UI
            SimConfigRow("Players", playerCount, 3, 8,
                onMinus = { playerCount-- }, onPlus = { playerCount++ })
            Spacer(modifier = Modifier.height(8.dp))
            SimConfigRow("Rounds", roundCount, 1, 10,
                onMinus = { roundCount-- }, onPlus = { roundCount++ })
            Spacer(modifier = Modifier.height(8.dp))
            SimConfigRow("Games", gameCount, 1, 100,
                onMinus = { gameCount-- }, onPlus = { gameCount++ })

            Spacer(modifier = Modifier.height(24.dp))

            if (isRunning) {
                Text(
                    text = "Running simulation...",
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 16.sp,
                    modifier = Modifier.align(ComposeAlignment.CenterHorizontally)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            MeowfiaPrimaryButton(
                text = if (gameCount == 1) "Run Simulation" else "Run $gameCount Games",
                enabled = !isRunning,
                onClick = {
                    isRunning = true
                    RoleRegistry.initialize()
                    FlowerRegistry.initialize()

                    val config = SimConfig(
                        seed = System.currentTimeMillis(),
                        playerCount = playerCount,
                        roundCount = roundCount,
                        verbosity = Verbosity.FULL
                    )

                    if (gameCount == 1) {
                        result = SimEngine(config).runGame()
                    } else {
                        // Run multiple, show the last one with full detail
                        var lastResult: SimGameResult? = null
                        for (i in 0 until gameCount) {
                            val c = config.copy(seed = config.seed!! + i)
                            lastResult = SimEngine(c).runGame()
                        }
                        result = lastResult
                    }
                    isRunning = false
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            MeowfiaSecondaryButton(text = "Back", onClick = onBack)
        } else {
            // Results UI
            SimResultsView(result = result!!, onBack = {
                result = null
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
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
private fun androidx.compose.foundation.layout.ColumnScope.SimResultsView(
    result: SimGameResult,
    onBack: () -> Unit
) {
    // Summary header
    val scores = result.finalScores.mapIndexed { i, s -> result.strategies[i] to s }
    val winner = scores.maxByOrNull { it.second }

    Text(
        text = "Game Complete — Seed: ${result.seed}",
        color = MeowfiaColors.TextSecondary,
        fontSize = 13.sp
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Final scores card
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(1.dp, MeowfiaColors.Primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Final Scores", color = MeowfiaColors.Primary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
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
    }

    Spacer(modifier = Modifier.height(12.dp))

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
            // Round header
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

            // Pool
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

            // Meowfia count
            Text(
                "Meowfia: ${roundLog.meowfiaCount}",
                color = MeowfiaColors.Meowfia,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Player assignments
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

            // Voting summary
            if (vr != null) {
                Spacer(modifier = Modifier.height(6.dp))
                val eliminatedName = strategies.getOrElse(vr.eliminatedId) { "P${vr.eliminatedId}" }
                Text(
                    "Eliminated: $eliminatedName (${vr.eliminatedAlignment.displayName})",
                    color = MeowfiaColors.Secondary,
                    fontSize = 12.sp
                )

                // Vote counts
                val voteStr = vr.votes.entries
                    .sortedByDescending { it.value }
                    .joinToString("  ") { (pid, votes) ->
                        "${strategies.getOrElse(pid) { "P$pid" }}:$votes"
                    }
                Text("Votes: $voteStr", color = MeowfiaColors.TextSecondary, fontSize = 11.sp)
            }

            // Post-round scores
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
