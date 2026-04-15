package com.meowfia.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.theme.MeowfiaColors

/**
 * Full-screen overlay showing a large role card.
 * Tapping anywhere dismisses it.
 */
@Composable
fun RoleCardOverlay(
    roleId: RoleId?,
    onDismiss: () -> Unit
) {
    if (roleId == null) return

    val borderColor = when (roleId.cardType) {
        CardType.FARM_ANIMAL -> MeowfiaColors.Farm
        CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
        CardType.FLOWER -> MeowfiaColors.Confused
    }
    val typeLabel = when (roleId.cardType) {
        CardType.FARM_ANIMAL -> "Farm Animal"
        CardType.MEOWFIA_ANIMAL -> "Meowfia Animal"
        CardType.FLOWER -> "Flower"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click so it doesn't dismiss */ },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MeowfiaColors.Surface),
            border = BorderStroke(3.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RoleIcon(roleId = roleId, size = 96)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = roleId.displayName,
                    color = MeowfiaColors.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = typeLabel,
                    color = borderColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = roleId.description,
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
    }
}
