package com.meowfia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.ui.components.HandoffGate
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun RoleRevealScreen(
    playerNames: List<String>,
    assignments: List<PlayerAssignment>,
    currentPlayerIndex: Int,
    onPlayerDone: () -> Unit
) {
    if (currentPlayerIndex >= assignments.size) return

    val assignment = assignments[currentPlayerIndex]
    val playerName = playerNames[assignment.playerId]

    HandoffGate(
        playerName = playerName,
        profileImage = com.meowfia.app.engine.GameSession.profileImages[assignment.playerId],
        onComplete = onPlayerDone
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = ComposeAlignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = playerName,
                color = MeowfiaColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "You are",
                color = MeowfiaColors.TextSecondary,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = assignment.alignment.displayName,
                color = if (assignment.alignment == Alignment.FARM) MeowfiaColors.Farm
                else MeowfiaColors.Meowfia,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = assignment.roleId.displayName,
                color = MeowfiaColors.Primary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = assignment.roleId.description,
                color = MeowfiaColors.TextSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
