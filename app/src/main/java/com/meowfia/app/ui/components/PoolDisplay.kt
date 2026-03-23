package com.meowfia.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.PoolCard
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.theme.MeowfiaColors

/** Horizontal row of pool cards with colored borders indicating card type. */
@Composable
fun PoolDisplay(
    pool: List<PoolCard>,
    onCardTapped: (RoleId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(pool) { card ->
            val borderColor = when (card.cardType) {
                CardType.FARM_ANIMAL -> MeowfiaColors.Farm
                CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
                CardType.FLOWER -> MeowfiaColors.Confused
            }

            Card(
                modifier = Modifier
                    .width(100.dp)
                    .clickable { onCardTapped(card.roleId) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
                border = BorderStroke(2.dp, borderColor)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = card.roleId.displayName,
                        color = MeowfiaColors.TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = when (card.cardType) {
                            CardType.FARM_ANIMAL -> "Farm"
                            CardType.MEOWFIA_ANIMAL -> "Meowfia"
                            CardType.FLOWER -> "Flower"
                        },
                        color = borderColor,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
