package com.meowfia.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.theme.MeowfiaColors

// Pre-computed colors
private val FARM_BG = Color(0xFF2E7D32)
private val MEOWFIA_BG = Color(0xFFC62828)
private val FLOWER_BG = Color(0xFF6A1B9A)

/** Static cache of role icon bitmaps — for contexts that need Bitmap (e.g. notifications). */
object RoleIconCache {
    private val bitmapCache = mutableMapOf<RoleId, Bitmap>()
    private val imageBitmapCache = mutableMapOf<RoleId, ImageBitmap>()

    fun getBitmap(roleId: RoleId): Bitmap {
        return bitmapCache.getOrPut(roleId) { renderRoleIcon(roleId) }
    }

    fun getImageBitmap(roleId: RoleId): ImageBitmap {
        return imageBitmapCache.getOrPut(roleId) { getBitmap(roleId).asImageBitmap() }
    }

    private fun renderRoleIcon(roleId: RoleId, size: Int = 200): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgColor = when (roleId.cardType) {
            CardType.FARM_ANIMAL -> 0xFF2E7D32.toInt()
            CardType.MEOWFIA_ANIMAL -> 0xFFC62828.toInt()
            CardType.FLOWER -> 0xFF6A1B9A.toInt()
        }
        val bgPaint = Paint().apply { color = bgColor; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, bgPaint)
        val initials = roleId.displayName.split(" ").take(2).joinToString("") { it.first().uppercase() }
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = size * (if (initials.length > 1) 0.33f else 0.45f)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        canvas.drawText(initials, size / 2f, size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f, textPaint)
        return bitmap
    }

    // Pre-computed initials cache
    private val initialsCache = mutableMapOf<RoleId, String>()
    fun getInitials(roleId: RoleId): String {
        return initialsCache.getOrPut(roleId) {
            roleId.displayName.split(" ").take(2).joinToString("") { it.first().uppercase() }
        }
    }
}

/** Generate a placeholder role icon bitmap (for external use). */
fun generateRoleIcon(roleId: RoleId, size: Int = 200): Bitmap {
    return RoleIconCache.getBitmap(roleId)
}

/**
 * Lightweight role icon — pure Compose, no bitmap rendering.
 * Uses a colored circle Box with Text initials.
 */
@Composable
fun RoleIcon(
    roleId: RoleId,
    modifier: Modifier = Modifier,
    size: Int = 48
) {
    val bg = when (roleId.cardType) {
        CardType.FARM_ANIMAL -> FARM_BG
        CardType.MEOWFIA_ANIMAL -> MEOWFIA_BG
        CardType.FLOWER -> FLOWER_BG
    }
    val initials = RoleIconCache.getInitials(roleId)
    val fontSize = if (initials.length > 1) (size * 0.3f).sp else (size * 0.4f).sp

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, bg, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
