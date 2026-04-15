package com.meowfia.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.theme.MeowfiaColors

private val FARM_COLOR = MeowfiaColors.Farm
private val MEOWFIA_COLOR = MeowfiaColors.Meowfia
private val FLOWER_COLOR = MeowfiaColors.Confused
private val FARM_BG = MeowfiaColors.Farm.copy(alpha = 0.15f)
private val MEOWFIA_BG = MeowfiaColors.Meowfia.copy(alpha = 0.15f)
private val FLOWER_BG = MeowfiaColors.Confused.copy(alpha = 0.15f)
private val FARM_BORDER_DIM = MeowfiaColors.Farm.copy(alpha = 0.4f)
private val MEOWFIA_BORDER_DIM = MeowfiaColors.Meowfia.copy(alpha = 0.4f)
private val FLOWER_BORDER_DIM = MeowfiaColors.Confused.copy(alpha = 0.4f)
private val SURFACE_BG = MeowfiaColors.SurfaceElevated
private val CARD_SHAPE = RoundedCornerShape(10.dp)

/**
 * Non-lazy grid of role items. With ~40 items this is faster than LazyVerticalGrid
 * because there's no lazy measurement/composition overhead per frame.
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

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for ((type, typeRoles) in grouped) {
            val label = when (type) {
                CardType.FARM_ANIMAL -> "Farm Animals"
                CardType.MEOWFIA_ANIMAL -> "Meowfia"
                CardType.FLOWER -> "Flowers"
            }
            val labelColor = typeColor(type)
            val allOfTypeSelected = typeRoles.all { it in selectedRoles }

            // Category header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (onCategoryTap != null) Modifier.clickable { onCategoryTap(type) } else Modifier)
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = labelColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (onCategoryTap != null) {
                    Text(
                        if (allOfTypeSelected) "tap to clear" else "tap to add all",
                        color = MeowfiaColors.TextSecondary, fontSize = 11.sp
                    )
                }
            }

            // Rows of 4
            val chunked = typeRoles.chunked(4)
            for (row in chunked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (role in row) {
                        Box(modifier = Modifier.weight(1f)) {
                            RoleGridItem(
                                role = role,
                                isSelected = role in selectedRoles,
                                onTap = { onRoleTap(role) },
                                onLongPress = { onRoleLongPress(role) }
                            )
                        }
                    }
                    // Fill empty slots in last row
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoleGridItem(
    role: RoleId,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val bgColor = if (isSelected) typeBg(role.cardType) else SURFACE_BG
    val borderColor = if (isSelected) typeColor(role.cardType) else typeBorderDim(role.cardType)
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val textColor = if (isSelected) typeColor(role.cardType) else MeowfiaColors.TextPrimary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(CARD_SHAPE)
            .background(bgColor)
            .border(borderWidth, borderColor, CARD_SHAPE)
            .combinedClickable(onClick = onTap, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            RoleIcon(roleId = role, size = 36)
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = role.displayName,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 12.sp
            )
        }
    }
}

private fun typeColor(type: CardType): Color = when (type) {
    CardType.FARM_ANIMAL -> FARM_COLOR
    CardType.MEOWFIA_ANIMAL -> MEOWFIA_COLOR
    CardType.FLOWER -> FLOWER_COLOR
}

private fun typeBg(type: CardType): Color = when (type) {
    CardType.FARM_ANIMAL -> FARM_BG
    CardType.MEOWFIA_ANIMAL -> MEOWFIA_BG
    CardType.FLOWER -> FLOWER_BG
}

private fun typeBorderDim(type: CardType): Color = when (type) {
    CardType.FARM_ANIMAL -> FARM_BORDER_DIM
    CardType.MEOWFIA_ANIMAL -> MEOWFIA_BORDER_DIM
    CardType.FLOWER -> FLOWER_BORDER_DIM
}
