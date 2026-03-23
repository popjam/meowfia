package com.meowfia.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.Alignment as GameAlignment
import com.meowfia.app.data.model.PlayerAssignment
import com.meowfia.app.ui.components.HandoffGate
import com.meowfia.app.ui.components.ProfilePicturePicker
import com.meowfia.app.ui.components.RevealCard
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.theme.MeowfiaColors

/**
 * Combined registration + role reveal screen.
 *
 * Each player sees a large card containing name entry + drawing canvas.
 * Dragging the card upward reveals their role, alignment, and description underneath.
 * Releasing the card snaps it back to hide the role.
 */
@Composable
fun PlayerRegistrationScreen(
    humanCount: Int,
    assignments: List<PlayerAssignment>,
    onAllRegistered: (names: List<String>, profiles: Map<Int, Bitmap>) -> Unit
) {
    val names = remember { mutableStateListOf<String>() }
    val profiles = remember { mutableStateMapOf<Int, Bitmap>() }
    var currentIndex by remember { mutableIntStateOf(0) }

    if (currentIndex >= humanCount) {
        onAllRegistered(names.toList(), profiles.toMap())
        return
    }

    val assignment = assignments.getOrNull(currentIndex)

    key(currentIndex) {
        val defaultName = "Player ${currentIndex + 1}"
        var currentName by remember { mutableStateOf(defaultName) }
        var hasEdited by remember { mutableStateOf(false) }
        var currentProfile by remember { mutableStateOf<Bitmap?>(null) }
        val focusManager = LocalFocusManager.current

        HandoffGate(
            playerName = "Player ${currentIndex + 1}",
            waitingMessage = "Pass the phone to the next player",
            showProfile = false,
            onComplete = {
                val name = currentName.trim().ifEmpty { defaultName }
                names.add(name)
                currentProfile?.let { profiles[currentIndex] = it }
                currentIndex++
            }
        ) {
            RevealCard(
                cardContent = {
                    // The card face — name entry + drawing canvas
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { focusManager.clearFocus() },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Player ${currentIndex + 1} of $humanCount",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Enter your name",
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = currentName,
                            onValueChange = { currentName = it; hasEdited = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    if (!hasEdited) { currentName = ""; hasEdited = true }
                                },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MeowfiaColors.Primary,
                                unfocusedBorderColor = MeowfiaColors.TextSecondary,
                                focusedTextColor = MeowfiaColors.TextPrimary,
                                unfocusedTextColor = MeowfiaColors.TextPrimary,
                                cursorColor = MeowfiaColors.Primary
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Profile picture",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        ProfilePicturePicker(
                            playerIndex = currentIndex,
                            onProfileChanged = { bitmap -> currentProfile = bitmap },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "↑ Drag up to peek at your role ↑",
                            color = MeowfiaColors.TextSecondary.copy(alpha = 0.6f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                },
                revealContent = {
                    // Secret role info revealed underneath the card
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
