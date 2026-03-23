package com.meowfia.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.ui.theme.MeowfiaColors

/** Large CAW CAW button with counter. Shifts color at 3/3. */
@Composable
fun CawCawButton(
    cawCount: Int,
    onCawCaw: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTriggered = cawCount >= 3
    val buttonColor = if (isTriggered) MeowfiaColors.Secondary else MeowfiaColors.Primary

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { if (!isTriggered) onCawCaw() },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            enabled = !isTriggered,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                contentColor = MeowfiaColors.TextOnPrimary,
                disabledContainerColor = MeowfiaColors.Secondary
            )
        ) {
            Text(
                text = "CAW CAW",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isTriggered) "3/3 — EARLY VOTE!" else "$cawCount/3",
            color = if (isTriggered) MeowfiaColors.Secondary else MeowfiaColors.TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
