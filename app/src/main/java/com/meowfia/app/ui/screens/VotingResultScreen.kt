package com.meowfia.app.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.PlayerPicker
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun VotingResultScreen(
    players: List<Player>,
    onEggsecuted: (eliminatedPlayerId: Int) -> Unit,
    eliminatedPlayer: Player?,
    winningTeam: Alignment?,
    onViewSummary: () -> Unit,
    profileImages: Map<Int, Bitmap> = emptyMap()
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
                profileImages = profileImages
            )

            MeowfiaPrimaryButton(
                text = "Confirm",
                onClick = { selectedPlayerId?.let { onEggsecuted(it) } },
                enabled = selectedPlayerId != null
            )
        } else {
            // Step 2: Reveal
            Spacer(modifier = Modifier.weight(1f))

            // Role icon for eliminated player
            RoleIcon(roleId = eliminatedPlayer.roleId, size = 120)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${eliminatedPlayer.name} was eggsecuted!",
                color = MeowfiaColors.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "They were a ${eliminatedPlayer.roleId.displayName}\non the ${eliminatedPlayer.alignment.displayName} team.",
                color = MeowfiaColors.TextSecondary,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (winningTeam != null) {
                Text(
                    text = "${winningTeam.displayName.uppercase()} WINS!",
                    color = if (winningTeam == Alignment.FARM) MeowfiaColors.Farm
                    else MeowfiaColors.Meowfia,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
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
