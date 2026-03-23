package com.meowfia.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun MeowfiaPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MeowfiaColors.Primary,
            contentColor = MeowfiaColors.TextOnPrimary,
            disabledContainerColor = MeowfiaColors.Primary.copy(alpha = 0.3f)
        )
    ) {
        Text(text.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MeowfiaSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, MeowfiaColors.Primary.copy(alpha = if (enabled) 1f else 0.3f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MeowfiaColors.SurfaceElevated,
            contentColor = MeowfiaColors.Primary,
            disabledContainerColor = MeowfiaColors.SurfaceElevated.copy(alpha = 0.3f),
            disabledContentColor = MeowfiaColors.Primary.copy(alpha = 0.3f)
        )
    ) {
        Text(
            text = text.uppercase(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MeowfiaColors.Primary
        )
    }
}
