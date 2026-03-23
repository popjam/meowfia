package com.meowfia.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.theme.MeowfiaColors

private val FARM_BG = 0xFF2E7D32.toInt()     // dark green
private val MEOWFIA_BG = 0xFFC62828.toInt()  // dark red
private val FLOWER_BG = 0xFF6A1B9A.toInt()   // purple

/**
 * Generate a placeholder role icon bitmap.
 * Colored circle with the first letter(s) of the role name.
 * Farm = green, Meowfia = red, Flower = purple.
 */
fun generateRoleIcon(roleId: RoleId, size: Int = 200): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val bgColor = when (roleId.cardType) {
        CardType.FARM_ANIMAL -> FARM_BG
        CardType.MEOWFIA_ANIMAL -> MEOWFIA_BG
        CardType.FLOWER -> FLOWER_BG
    }

    val bgPaint = Paint().apply {
        color = bgColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)

    // Draw initials (first 1-2 chars)
    val initials = roleId.displayName
        .split(" ")
        .take(2)
        .joinToString("") { it.first().uppercase() }

    val textPaint = Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = size * (if (initials.length > 1) 0.33f else 0.45f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isAntiAlias = true
    }
    val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
    canvas.drawText(initials, size / 2f, textY, textPaint)

    return bitmap
}

/**
 * Displays a role icon as a circular thumbnail.
 * Uses placeholder generated icons for now — will be replaced with real art later.
 */
@Composable
fun RoleIcon(
    roleId: RoleId,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    val bitmap = remember(roleId) { generateRoleIcon(roleId, 200) }
    val borderColor = when (roleId.cardType) {
        CardType.FARM_ANIMAL -> MeowfiaColors.Farm
        CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
        CardType.FLOWER -> MeowfiaColors.Confused
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = roleId.displayName,
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape),
        contentScale = ContentScale.Crop
    )
}
