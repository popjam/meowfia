package com.meowfia.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.theme.MeowfiaColors

/**
 * Grid of square player cards for selecting a target.
 * Each card shows a large profile picture with the name below.
 */
@Composable
fun PlayerPicker(
    players: List<Player>,
    excludePlayerId: Int? = null,
    selectedPlayerId: Int?,
    onPlayerSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    profileImages: Map<Int, Bitmap> = emptyMap()
) {
    val columns = when {
        players.size <= 4 -> 2
        else -> 3
    }

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

            OutlinedButton(
                onClick = { if (!isExcluded) onPlayerSelected(player.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f),
                enabled = !isExcluded,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = when {
                        isSelected -> MeowfiaColors.Primary
                        isExcluded -> MeowfiaColors.Dead
                        else -> MeowfiaColors.TextSecondary.copy(alpha = 0.4f)
                    }
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MeowfiaColors.Primary.copy(alpha = 0.15f)
                    else MeowfiaColors.Surface,
                    disabledContentColor = MeowfiaColors.Dead
                ),
                contentPadding = PaddingValues(8.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
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
                        color = when {
                            isExcluded -> MeowfiaColors.Dead
                            isSelected -> MeowfiaColors.Primary
                            else -> MeowfiaColors.TextPrimary
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
