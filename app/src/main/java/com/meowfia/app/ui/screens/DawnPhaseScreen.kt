package com.meowfia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.DawnReport
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.components.HandoffGate
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.PhaseHeader
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun DawnPhaseScreen(
    roundNumber: Int,
    players: List<Player>,
    currentPlayerIndex: Int,
    getDawnReport: (playerId: Int) -> DawnReport,
    onPlayerDone: () -> Unit,
    onAllDone: () -> Unit
) {
    if (currentPlayerIndex >= players.size) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Everyone open your eyes!\nPlace your nest eggs now.",
                color = MeowfiaColors.TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            MeowfiaPrimaryButton(text = "Start Day", onClick = onAllDone)
        }
        return
    }

    val player = players[currentPlayerIndex]
    val report = getDawnReport(player.id)

    HandoffGate(
        playerName = player.name,
        profileImage = com.meowfia.app.engine.GameSession.profileImages[player.id],
        onComplete = onPlayerDone
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PhaseHeader(roundNumber = roundNumber, phaseName = "Dawn Phase")
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = player.name,
                color = MeowfiaColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your nest has",
                color = MeowfiaColors.TextSecondary,
                fontSize = 18.sp
            )
            Text(
                text = "${report.reportedNestEggs}",
                color = MeowfiaColors.Primary,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (report.reportedNestEggs == 1) "egg" else "eggs",
                color = MeowfiaColors.TextSecondary,
                fontSize = 18.sp
            )

            if (report.isConfused) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "(You're confused — this count may be inaccurate)",
                    color = MeowfiaColors.Confused,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (report.additionalInfo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                for (info in report.additionalInfo) {
                    Text(
                        text = info,
                        color = MeowfiaColors.TextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
