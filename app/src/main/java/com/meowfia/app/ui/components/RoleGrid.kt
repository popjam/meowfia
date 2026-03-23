package com.meowfia.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.theme.MeowfiaColors

/**
 * A grid of role icons + names, separated by type (Farm / Meowfia / Flower).
 * Tap calls [onRoleTap], long-press calls [onRoleLongPress].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoleGrid(
    roles: List<RoleId>,
    onRoleTap: (RoleId) -> Unit = {},
    onRoleLongPress: (RoleId) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val grouped = remember(roles) {
        val order = listOf(CardType.FARM_ANIMAL, CardType.MEOWFIA_ANIMAL, CardType.FLOWER)
        order.mapNotNull { type ->
            val items = roles.filter { it.cardType == type }
            if (items.isNotEmpty()) type to items else null
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((type, typeRoles) in grouped) {
            val label = when (type) {
                CardType.FARM_ANIMAL -> "Farm Animals"
                CardType.MEOWFIA_ANIMAL -> "Meowfia"
                CardType.FLOWER -> "Flowers"
            }
            val labelColor = when (type) {
                CardType.FARM_ANIMAL -> MeowfiaColors.Farm
                CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
                CardType.FLOWER -> MeowfiaColors.Confused
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = label,
                    color = labelColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(typeRoles, key = { it.name }) { role ->
                val borderColor = when (role.cardType) {
                    CardType.FARM_ANIMAL -> MeowfiaColors.Farm
                    CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
                    CardType.FLOWER -> MeowfiaColors.Confused
                }

                Card(
                    modifier = Modifier
                        .width(80.dp)
                        .combinedClickable(
                            onClick = { onRoleTap(role) },
                            onLongClick = { onRoleLongPress(role) }
                        ),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
                    border = BorderStroke(1.5.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RoleIcon(roleId = role, size = 40)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = role.displayName,
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}
