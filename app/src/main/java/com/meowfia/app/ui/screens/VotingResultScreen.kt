package com.meowfia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.PlayerPicker
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun VotingResultScreen(
    players: List<Player>,
    onEggsecuted: (eliminatedPlayerId: Int) -> Unit,
    eliminatedPlayer: Player?,
    winningTeam: Alignment?,
    onViewSummary: () -> Unit
) {
    var selectedPlayerId by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = ComposeAlignment.CenterHorizontally
    ) {
        if (eliminatedPlayer == null) {
            // Step 1: Dealer picks who was eliminated
            Text(
                text = "Who was eggsecuted?",
                color = MeowfiaColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            PlayerPicker(
                players = players,
                selectedPlayerId = selectedPlayerId,
                onPlayerSelected = { selectedPlayerId = it },
                modifier = Modifier.weight(1f),
                profileImages = com.meowfia.app.engine.GameSession.profileImages
            )

            MeowfiaPrimaryButton(
                text = "Confirm",
                onClick = { selectedPlayerId?.let { onEggsecuted(it) } },
                enabled = selectedPlayerId != null
            )
        } else {
            // Step 2: Reveal
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = eliminatedPlayer.name,
                color = MeowfiaColors.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = eliminatedPlayer.alignment.displayName,
                color = if (eliminatedPlayer.alignment == Alignment.FARM) MeowfiaColors.Farm
                else MeowfiaColors.Meowfia,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = eliminatedPlayer.roleId.displayName,
                color = MeowfiaColors.Primary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = eliminatedPlayer.roleId.description,
                color = MeowfiaColors.TextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (winningTeam != null) {
                Text(
                    text = "${winningTeam.displayName.uppercase()} WINS!",
                    color = if (winningTeam == Alignment.FARM) MeowfiaColors.Farm
                    else MeowfiaColors.Meowfia,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            MeowfiaPrimaryButton(
                text = "View Round Summary",
                onClick = onViewSummary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
