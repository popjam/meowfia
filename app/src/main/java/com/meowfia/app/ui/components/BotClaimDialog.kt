package com.meowfia.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.meowfia.app.bot.BotDayClaim
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun BotClaimDialog(
    claim: BotDayClaim,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${claim.botName} claims:",
                color = MeowfiaColors.Primary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = claim.toDisplayText(),
                color = MeowfiaColors.TextPrimary,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            MeowfiaPrimaryButton(
                text = "OK",
                onClick = onDismiss
            )
        },
        containerColor = MeowfiaColors.Surface
    )
}
