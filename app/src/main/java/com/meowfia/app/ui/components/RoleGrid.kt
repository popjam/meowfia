package com.meowfia.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * A grid of uniform-sized role cards, separated by type (Farm / Meowfia / Flower).
 * Tap calls [onRoleTap], long-press calls [onRoleLongPress].
 * Tapping a category header calls [onCategoryTap] to toggle all roles of that type.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoleGrid(
    roles: List<RoleId>,
    selectedRoles: Set<RoleId> = emptySet(),
    onRoleTap: (RoleId) -> Unit = {},
    onRoleLongPress: (RoleId) -> Unit = {},
    onCategoryTap: ((CardType) -> Unit)? = null,
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
            val allOfTypeSelected = typeRoles.all { it in selectedRoles }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (onCategoryTap != null) Modifier.clickable { onCategoryTap(type) }
                            else Modifier
                        )
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        color = labelColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (onCategoryTap != null) {
                        Text(
                            text = if (allOfTypeSelected) "tap to clear" else "tap to add all",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            items(typeRoles, key = { it.name }) { role ->
                val isSelected = role in selectedRoles
                val borderColor = when (role.cardType) {
                    CardType.FARM_ANIMAL -> MeowfiaColors.Farm
                    CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
                    CardType.FLOWER -> MeowfiaColors.Confused
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .combinedClickable(
                            onClick = { onRoleTap(role) },
                            onLongClick = { onRoleLongPress(role) }
                        ),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            borderColor.copy(alpha = 0.15f)
                        else
                            MeowfiaColors.SurfaceElevated
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) borderColor else borderColor.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        RoleIcon(roleId = role, size = 36)
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = role.displayName,
                            color = if (isSelected) borderColor else MeowfiaColors.TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 12.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
