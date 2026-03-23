package com.meowfia.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.ui.theme.MeowfiaColors

/** Countdown timer bar with large time display. Progress goes from 1.0 to 0.0. */
@Composable
fun TimerBar(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds else 0f
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timerColor = if (progress > 0.2f) MeowfiaColors.Primary else MeowfiaColors.Secondary

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%d:%02d".format(minutes, seconds),
            color = timerColor,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = timerColor,
            trackColor = MeowfiaColors.Surface,
        )
    }
}
