package com.meowfia.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.theme.MeowfiaColors

/** Card showing a role icon, name, alignment color, and description. */
@Composable
fun RoleInfoCard(
    roleId: RoleId,
    modifier: Modifier = Modifier
) {
    val borderColor = when (roleId.cardType) {
        CardType.FARM_ANIMAL -> MeowfiaColors.Farm
        CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
        CardType.FLOWER -> MeowfiaColors.Confused
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoleIcon(roleId = roleId, size = 48)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roleId.displayName,
                    color = MeowfiaColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (roleId.cardType) {
                        CardType.FARM_ANIMAL -> "Farm"
                        CardType.MEOWFIA_ANIMAL -> "Meowfia"
                        CardType.FLOWER -> "Flower"
                    },
                    color = borderColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = roleId.description,
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 15.sp
                )
            }
        }
    }
}
