package com.meowfia.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.ui.theme.MeowfiaColors
import kotlinx.coroutines.delay

@Composable
fun BotActionScreen(
    botName: String,
    phaseName: String,
    actionText: String,
    delayMs: Long = 1500L,
    onComplete: () -> Unit
) {
    LaunchedEffect(botName) {
        delay(delayMs)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = phaseName,
            color = MeowfiaColors.TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = botName,
            color = MeowfiaColors.Primary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = actionText,
            color = MeowfiaColors.TextSecondary,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}
