package com.meowfia.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment as GameAlignment
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.ui.components.HandoffGate
import com.meowfia.app.ui.components.RevealCard
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.theme.MeowfiaColors

/**
 * Role reveal screen. Each human player sees a card they can drag up
 * to peek at their assigned role, alignment, and description.
 * Player names and profiles are configured in the pool setup screen.
 */
@Composable
fun PlayerRegistrationScreen(
    humanCount: Int,
    assignments: List<PlayerAssignment>,
    playerNames: List<String> = emptyList(),
    onAllDone: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    if (currentIndex >= humanCount) {
        onAllDone()
        return
    }

    val assignment = assignments.getOrNull(currentIndex)
    val playerName = playerNames.getOrElse(currentIndex) { "Player ${currentIndex + 1}" }

    key(currentIndex) {
        HandoffGate(
            playerName = playerName,
            waitingMessage = "Pass the phone to:",
            showProfile = true,
            profileImage = com.meowfia.app.engine.GameSession.profileImages[currentIndex],
            onComplete = { currentIndex++ }
        ) {
            RevealCard(
                cardContent = {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Player ${currentIndex + 1} of $humanCount",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = playerName,
                            color = MeowfiaColors.Primary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Your role is hidden below",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        Text(
                            text = "Drag up to peek at your role",
                            color = MeowfiaColors.TextSecondary.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                revealContent = {
                    if (assignment != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            RoleIcon(roleId = assignment.roleId, size = 80)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = assignment.roleId.displayName,
                                color = MeowfiaColors.Primary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "You are on the ${assignment.alignment.displayName} team",
                                color = if (assignment.alignment == GameAlignment.FARM)
                                    MeowfiaColors.Farm else MeowfiaColors.Meowfia,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = assignment.roleId.description,
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            )
        }
    }
}
