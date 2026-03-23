package com.meowfia.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.meowfia.app.ui.theme.MeowfiaColors
import kotlin.math.roundToInt

/**
 * A full-size card that covers secret content underneath.
 * The card displays [cardContent] (name input, canvas, etc.) on its face.
 * Dragging the card upward reveals [revealContent] (role info) behind it.
 * Releasing snaps the card back down.
 */
@Composable
fun RevealCard(
    modifier: Modifier = Modifier,
    cardContent: @Composable () -> Unit,
    revealContent: @Composable () -> Unit
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue = if (isDragging) dragOffset else 0f,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 250),
        label = "cardOffset"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Secret content behind — always rendered, visible when card is dragged up
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            revealContent()
        }

        // The draggable card on top — takes up the full space
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, animatedOffset.roundToInt()) }
                .shadow(12.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(MeowfiaColors.SurfaceElevated)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffset = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = dragOffset + dragAmount
                            dragOffset = newOffset.coerceAtMost(0f)
                        }
                    )
                }
        ) {
            cardContent()
        }
    }
}
