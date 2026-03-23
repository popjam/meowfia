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
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun StartScreen(
    onNewGame: () -> Unit,
    onViewRoles: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "MEOWFIA",
            color = MeowfiaColors.Primary,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Nobody knows who the cats are\n— not even the cats.",
            color = MeowfiaColors.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        MeowfiaPrimaryButton(
            text = "New Game",
            onClick = onNewGame
        )
        Spacer(modifier = Modifier.height(12.dp))
        MeowfiaSecondaryButton(
            text = "View Roles",
            onClick = onViewRoles
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "3-8 players · 1 phone · lots of eggs",
            color = MeowfiaColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
