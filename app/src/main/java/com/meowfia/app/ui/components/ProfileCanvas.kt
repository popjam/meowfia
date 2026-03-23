package com.meowfia.app.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.ui.theme.MeowfiaColors

/**
 * A small drawing canvas where players can draw a black-and-white profile picture.
 * Includes a circular clear button below the canvas.
 */
@Composable
fun ProfileCanvas(
    onDrawingChanged: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var clearVersion by remember { mutableIntStateOf(0) }

    fun renderBitmap(strokeList: List<List<Offset>>, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bg = Paint().apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bg)

        val paint = Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = size / 24f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        for (stroke in strokeList) {
            if (stroke.size < 2) {
                if (stroke.size == 1) {
                    canvas.drawCircle(stroke[0].x, stroke[0].y, paint.strokeWidth / 2,
                        paint.apply { style = Paint.Style.FILL })
                    paint.style = Paint.Style.STROKE
                }
                continue
            }
            val path = Path()
            path.moveTo(stroke[0].x, stroke[0].y)
            for (i in 1 until stroke.size) {
                path.lineTo(stroke[i].x, stroke[i].y)
            }
            canvas.drawPath(path, paint)
        }
        return bmp
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(2.dp, MeowfiaColors.Primary, RoundedCornerShape(12.dp))
                .clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(clearVersion) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentStroke = listOf(offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            currentStroke = currentStroke + change.position
                        },
                        onDragEnd = {
                            strokes.add(currentStroke)
                            currentStroke = emptyList()
                            if (canvasSize.width > 0) {
                                val size = maxOf(canvasSize.width, canvasSize.height)
                                onDrawingChanged(renderBitmap(strokes.toList(), size))
                            }
                        }
                    )
                }
        ) {
            if (canvasSize.width > 0 && canvasSize.height > 0) {
                val allStrokes = strokes.toList() +
                    if (currentStroke.isNotEmpty()) listOf(currentStroke) else emptyList()
                val size = maxOf(canvasSize.width, canvasSize.height)
                val bitmap = remember(allStrokes.size, currentStroke.size, clearVersion) {
                    renderBitmap(allStrokes, size)
                }

                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Profile drawing",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Clear button
        IconButton(
            onClick = {
                strokes.clear()
                currentStroke = emptyList()
                clearVersion++
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.5.dp, MeowfiaColors.Secondary, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MeowfiaColors.Surface
            )
        ) {
            Text(
                text = "X",
                color = MeowfiaColors.Secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Displays a player's profile drawing as a small circular thumbnail. */
@Composable
fun ProfileThumbnail(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    size: Int = 64
) {
    if (bitmap != null) {
        val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
        Image(
            bitmap = imageBitmap,
            contentDescription = "Player profile",
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .border(1.dp, MeowfiaColors.Primary, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(MeowfiaColors.SurfaceElevated)
                .border(1.dp, MeowfiaColors.TextSecondary, CircleShape)
        )
    }
}
