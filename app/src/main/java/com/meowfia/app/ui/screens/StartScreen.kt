package com.meowfia.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun StartScreen(
    onNewGame: () -> Unit,
    onViewRoles: () -> Unit,
    onSimulation: (() -> Unit)? = null
) {
    var titleTapCount by remember { mutableIntStateOf(0) }
    val devMode = titleTapCount >= 5

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
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { titleTapCount++ }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Nobody knows who the cats are\n— not even the cats.",
            color = MeowfiaColors.TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        if (devMode && onSimulation != null) {
            MeowfiaSecondaryButton(
                text = "Simulation Mode",
                onClick = onSimulation
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

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
            text = if (devMode) "Developer mode enabled" else "3-8 players · 1 phone · lots of eggs",
            color = if (devMode) MeowfiaColors.Primary else MeowfiaColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
