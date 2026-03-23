package com.meowfia.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.ui.theme.MeowfiaColors

enum class ProfileMode { COLOR, DRAW, PHOTO }

private val PROFILE_COLORS = listOf(
    0xFFE94560.toInt(), // red
    0xFF4CAF50.toInt(), // green
    0xFF2196F3.toInt(), // blue
    0xFFF5A623.toInt(), // orange/gold
    0xFF9C27B0.toInt(), // purple
    0xFFFF9800.toInt(), // amber
    0xFF00BCD4.toInt(), // teal
    0xFFE91E63.toInt(), // pink
)

fun generateColorProfile(playerIndex: Int, size: Int = 200): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val color = PROFILE_COLORS[playerIndex % PROFILE_COLORS.size]
    val paint = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Draw initial letter
    val letterPaint = Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = size * 0.45f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    val label = "${playerIndex + 1}"
    val textY = size / 2f - (letterPaint.descent() + letterPaint.ascent()) / 2f
    canvas.drawText(label, size / 2f, textY, letterPaint)

    return bitmap
}

@Composable
fun ProfilePicturePicker(
    playerIndex: Int,
    onProfileChanged: (Bitmap?) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(ProfileMode.COLOR) }

    // Auto-generate color profile on first composition and when switching to color mode
    LaunchedEffect(mode, playerIndex) {
        if (mode == ProfileMode.COLOR) {
            onProfileChanged(generateColorProfile(playerIndex))
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode selector — 3 pill buttons in a row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ProfileModeButton(
                label = "Color",
                selected = mode == ProfileMode.COLOR,
                onClick = {
                    mode = ProfileMode.COLOR
                    onProfileChanged(generateColorProfile(playerIndex))
                },
                modifier = Modifier.weight(1f)
            )
            ProfileModeButton(
                label = "Draw",
                selected = mode == ProfileMode.DRAW,
                onClick = {
                    mode = ProfileMode.DRAW
                    onProfileChanged(null)
                },
                modifier = Modifier.weight(1f)
            )
            ProfileModeButton(
                label = "Photo",
                selected = mode == ProfileMode.PHOTO,
                onClick = {
                    mode = ProfileMode.PHOTO
                    onProfileChanged(null)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mode content
        when (mode) {
            ProfileMode.COLOR -> {
                // Show the generated color circle as a preview
                val bitmap = remember(playerIndex) { generateColorProfile(playerIndex) }
                ProfileThumbnail(bitmap = bitmap, size = 120)
            }
            ProfileMode.DRAW -> {
                ProfileCanvas(
                    onDrawingChanged = { bitmap -> onProfileChanged(bitmap) },
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
            ProfileMode.PHOTO -> {
                CameraSelfie(
                    onPhotoCaptured = { bitmap -> onProfileChanged(bitmap) },
                    modifier = Modifier.fillMaxWidth(0.6f)
                )
            }
        }
    }
}

@Composable
private fun ProfileModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MeowfiaColors.Primary else MeowfiaColors.Surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MeowfiaColors.Primary else MeowfiaColors.TextSecondary.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = label,
            color = if (selected) MeowfiaColors.TextOnPrimary else MeowfiaColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )
    }
}
