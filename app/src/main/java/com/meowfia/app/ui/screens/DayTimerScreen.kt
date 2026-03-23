package com.meowfia.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.bot.BotDayClaim
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.components.CawCawButton
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.PhaseHeader
import com.meowfia.app.ui.components.TimerBar
import com.meowfia.app.ui.theme.MeowfiaColors
import kotlinx.coroutines.delay

private const val DAY_PHASE_SECONDS = 300 // 5 minutes

@Composable
fun DayTimerScreen(
    roundNumber: Int,
    cawCawCount: Int,
    activeFlowers: List<RoleId>,
    botClaims: List<BotDayClaim> = emptyList(),
    onCawCaw: () -> Unit,
    onTimeUp: () -> Unit
) {
    var remainingSeconds by remember { mutableIntStateOf(DAY_PHASE_SECONDS) }
    val isTimeUp = remainingSeconds <= 0 || cawCawCount >= 3

    LaunchedEffect(Unit) {
        while (remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhaseHeader(roundNumber = roundNumber, phaseName = "Day Phase")
        Spacer(modifier = Modifier.height(16.dp))

        TimerBar(
            remainingSeconds = remainingSeconds,
            totalSeconds = DAY_PHASE_SECONDS
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Flower reminders
        if (RoleId.SUNFLOWER in activeFlowers) {
            Text(
                text = "Sunflower: One player may reveal their role to gain 2 eggs.",
                color = MeowfiaColors.Confused,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bot claims — persistent scrollable list
        if (botClaims.isNotEmpty()) {
            Text(
                text = "Bot Claims",
                color = MeowfiaColors.TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(botClaims) { claim ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
                        border = BorderStroke(1.dp, MeowfiaColors.Primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${claim.botName}:",
                                color = MeowfiaColors.Primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = claim.toDisplayText(),
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isTimeUp) {
            Text(
                text = if (cawCawCount >= 3) "CAW CAW — EARLY VOTE!" else "TIME'S UP — VOTE!",
                color = MeowfiaColors.Secondary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            MeowfiaPrimaryButton(text = "Begin Voting", onClick = onTimeUp)
        } else {
            CawCawButton(cawCount = cawCawCount, onCawCaw = onCawCaw)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
