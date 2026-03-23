package com.meowfia.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.theme.MeowfiaColors
import kotlin.math.abs

@Composable
fun RoundSummaryScreen(
    roundNumber: Int,
    players: List<Player>,
    visitGraph: Map<Int, Int?>,
    dawnReports: Map<Int, DawnReport>,
    eliminatedPlayerId: Int?,
    winningTeam: Alignment?,
    onNextRound: () -> Unit,
    onEndGame: () -> Unit
) {
    val winners = remember(winningTeam, players) {
        if (winningTeam != null) players.filter { it.alignment == winningTeam } else emptyList()
    }
    val losers = remember(winningTeam, players) {
        if (winningTeam != null) players.filter { it.alignment != winningTeam } else players
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Round $roundNumber Summary",
            color = MeowfiaColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            // Winners section
            if (winners.isNotEmpty()) {
                item {
                    Text(
                        text = "WINNERS (${winningTeam?.displayName ?: ""})",
                        color = if (winningTeam == Alignment.FARM) MeowfiaColors.Farm else MeowfiaColors.Meowfia,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(winners, key = { it.id }) { player ->
                    PlayerSummaryCard(
                        player = player,
                        visitGraph = visitGraph,
                        dawnReports = dawnReports,
                        players = players,
                        isEliminated = player.id == eliminatedPlayerId,
                        isWinner = true
                    )
                }
            }

            // Losers section
            if (losers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (winningTeam == null) "No Meowfia — everyone loses!" else "LOSERS (${if (winningTeam == Alignment.FARM) "Meowfia" else "Farm"})",
                        color = MeowfiaColors.TextSecondary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(losers, key = { it.id }) { player ->
                    PlayerSummaryCard(
                        player = player,
                        visitGraph = visitGraph,
                        dawnReports = dawnReports,
                        players = players,
                        isEliminated = player.id == eliminatedPlayerId,
                        isWinner = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        MeowfiaPrimaryButton(text = "Next Round", onClick = onNextRound)
        Spacer(modifier = Modifier.height(8.dp))
        MeowfiaSecondaryButton(text = "End Game", onClick = onEndGame)
    }
}

@Composable
private fun PlayerSummaryCard(
    player: Player,
    visitGraph: Map<Int, Int?>,
    dawnReports: Map<Int, DawnReport>,
    players: List<Player>,
    isEliminated: Boolean,
    isWinner: Boolean
) {
    val visitedName = visitGraph[player.id]?.let { targetId ->
        players.find { it.id == targetId }?.name
    } ?: "nobody"

    val report = dawnReports[player.id]
    val eggDelta = report?.actualEggDelta ?: 0
    val eggText = when {
        eggDelta > 0 -> "+$eggDelta egg${if (eggDelta != 1) "s" else ""}"
        eggDelta < 0 -> "$eggDelta egg${if (abs(eggDelta) != 1) "s" else ""}"
        else -> "no egg change"
    }

    val alignmentColor = if (player.alignment == Alignment.FARM)
        MeowfiaColors.Farm else MeowfiaColors.Meowfia

    val botPrefix = if (player.isBot) "[BOT] " else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(1.dp, alignmentColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = "$botPrefix${player.name}",
                        color = MeowfiaColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isEliminated) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Eliminated",
                            color = MeowfiaColors.Secondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row {
                    Text(
                        text = player.alignment.displayName,
                        color = alignmentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = player.roleId.displayName,
                        color = MeowfiaColors.Primary,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "Visited: $visitedName · $eggText",
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            RoleIcon(roleId = player.roleId, size = 44)
        }
    }
}
