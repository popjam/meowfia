package com.meowfia.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

/**
 * Pass-and-play handoff gate. Two states:
 * 1. WAITING — "Pass the phone to [playerName]" with their profile pic, tap to reveal
 * 2. REVEALED — Shows secret content, tap "Done" to complete and advance
 */
@Composable
fun HandoffGate(
    playerName: String,
    profileImage: Bitmap? = null,
    waitingMessage: String = "Pass the phone to:",
    showProfile: Boolean = true,
    doneEnabled: Boolean = true,
    onComplete: () -> Unit,
    content: @Composable () -> Unit
) {
    var state by remember { mutableStateOf(GateState.WAITING) }

    when (state) {
        GateState.WAITING -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = waitingMessage,
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (showProfile) {
                    ProfileThumbnail(
                        bitmap = profileImage,
                        size = 140
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = playerName,
                    color = MeowfiaColors.Primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(48.dp))
                MeowfiaPrimaryButton(
                    text = "Tap When Ready",
                    onClick = { state = GateState.REVEALED }
                )
            }
        }

        GateState.REVEALED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
                MeowfiaPrimaryButton(
                    text = "Done",
                    enabled = doneEnabled,
                    onClick = {
                        state = GateState.WAITING
                        onComplete()
                    }
                )
            }
        }
    }
}

private enum class GateState {
    WAITING, REVEALED
}
