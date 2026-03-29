package com.meowfia.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.theme.MeowfiaColors

private val PICKER_SHAPE = RoundedCornerShape(14.dp)
private val SELECTED_BG = MeowfiaColors.Primary.copy(alpha = 0.15f)
private val DIM_BORDER = MeowfiaColors.TextSecondary.copy(alpha = 0.4f)

@Composable
fun PlayerPicker(
    players: List<Player>,
    excludePlayerId: Int? = null,
    selectedPlayerId: Int?,
    onPlayerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    profileImages: Map<Int, Bitmap> = emptyMap()
) {
    val columns = if (players.size <= 4) 2 else 3

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(players, key = { it.id }) { player ->
            val isExcluded = player.id == excludePlayerId
            val isSelected = player.id == selectedPlayerId
            val bgColor = when {
                isSelected -> SELECTED_BG
                else -> MeowfiaColors.Surface
            }
            val borderColor = when {
                isSelected -> MeowfiaColors.Primary
                isExcluded -> MeowfiaColors.Dead
                else -> DIM_BORDER
            }
            val textColor = when {
                isExcluded -> MeowfiaColors.Dead
                isSelected -> MeowfiaColors.Primary
                else -> MeowfiaColors.TextPrimary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .clip(PICKER_SHAPE)
                    .background(bgColor)
                    .border(if (isSelected) 3.dp else 1.dp, borderColor, PICKER_SHAPE)
                    .clickable(enabled = !isExcluded) { onPlayerSelected(player.id) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    ProfileThumbnail(
                        bitmap = profileImages[player.id],
                        size = 72
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = player.name,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
