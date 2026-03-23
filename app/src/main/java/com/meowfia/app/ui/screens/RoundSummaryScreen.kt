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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun RoundSummaryScreen(
    roundNumber: Int,
    players: List<Player>,
    visitGraph: Map<Int, Int?>,
    onNextRound: () -> Unit,
    onEndGame: () -> Unit
) {
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
            items(players) { player ->
                val visitedName = visitGraph[player.id]?.let { targetId ->
                    players.find { it.id == targetId }?.name
                } ?: "nobody"

                val alignmentColor = if (player.alignment == Alignment.FARM)
                    MeowfiaColors.Farm else MeowfiaColors.Meowfia

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
                    border = BorderStroke(1.dp, alignmentColor.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = player.name,
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                                text = "Visited: $visitedName",
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        MeowfiaPrimaryButton(text = "Next Round", onClick = onNextRound)
        Spacer(modifier = Modifier.height(8.dp))
        MeowfiaSecondaryButton(text = "End Game", onClick = onEndGame)
    }
}
