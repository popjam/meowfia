package com.meowfia.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Player
import com.meowfia.app.ui.theme.MeowfiaColors

/**
 * Grid of player buttons for selecting a target.
 * Shows profile thumbnail + name. Selected player highlighted in amber.
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
    val columns = if (players.size >= 6) 2 else 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(players, key = { it.id }) { player ->
            val isExcluded = player.id == excludePlayerId
            val isSelected = player.id == selectedPlayerId

            OutlinedButton(
                onClick = { if (!isExcluded) onPlayerSelected(player.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                enabled = !isExcluded,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = when {
                        isSelected -> MeowfiaColors.Primary
                        isExcluded -> MeowfiaColors.Dead
                        else -> MeowfiaColors.TextSecondary
                    }
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) MeowfiaColors.Primary.copy(alpha = 0.15f)
                    else MeowfiaColors.Surface,
                    disabledContentColor = MeowfiaColors.Dead
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileThumbnail(
                        bitmap = profileImages[player.id],
                        size = 36
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = player.name,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isExcluded -> MeowfiaColors.Dead
                            isSelected -> MeowfiaColors.Primary
                            else -> MeowfiaColors.TextPrimary
                        }
                    )
                }
            }
        }
    }
}
