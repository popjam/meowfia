package com.meowfia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.NightAction
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.registry.RoleRegistry
import com.meowfia.app.engine.GameSession
import com.meowfia.app.roles.NightPrompt
import com.meowfia.app.bot.BotBrain
import com.meowfia.app.ui.components.BotActionScreen
import com.meowfia.app.ui.components.HandoffGate
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.PhaseHeader
import com.meowfia.app.ui.components.PlayerPicker
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun NightPhaseScreen(
    roundNumber: Int,
    players: List<Player>,
    currentPlayerIndex: Int,
    onActionSubmitted: (playerId: Int, action: NightAction) -> Unit,
    onAllDone: () -> Unit
) {
    if (currentPlayerIndex >= players.size) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "All eyes closed.\nThe night is resolving...",
                color = MeowfiaColors.TextSecondary,
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            MeowfiaPrimaryButton(text = "Continue to Dawn", onClick = onAllDone)
        }
        return
    }

    val player = players[currentPlayerIndex]

    // Bot players: show intermediate screen, auto-submit action
    if (player.isBot) {
        BotActionScreen(
            botName = player.name,
            phaseName = "Night Phase",
            actionText = "is performing their night action",
            delayMs = 1500L,
            onComplete = {
                val action = BotBrain.chooseNightAction(
                    bot = player,
                    allPlayers = players,
                    random = GameSession.coordinator.randomProvider
                )
                onActionSubmitted(player.id, action)
            }
        )
        return
    }

    val handler = RoleRegistry.get(player.roleId)
    val prompt = handler.getNightPrompt(player, players)

    // Store the picked target so it can be submitted through onComplete
    var confirmedTarget by remember(currentPlayerIndex) { mutableStateOf<Int?>(null) }
    val hasConfirmed = confirmedTarget != null

    // key() ensures HandoffGate state resets for each player
    key(currentPlayerIndex) {
        HandoffGate(
            playerName = player.name,
            profileImage = GameSession.profileImages[player.id],
            onComplete = {
                when (prompt) {
                    is NightPrompt.Automatic ->
                        onActionSubmitted(player.id, NightAction.VisitRandom)
                    is NightPrompt.SelfVisit ->
                        onActionSubmitted(player.id, NightAction.VisitSelf)
                    is NightPrompt.PickPlayer -> {
                        confirmedTarget?.let { targetId ->
                            onActionSubmitted(player.id, NightAction.VisitPlayer(targetId))
                        }
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                PhaseHeader(roundNumber = roundNumber, phaseName = "Night Phase")
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = player.alignment.displayName,
                    color = if (player.alignment == com.meowfia.app.data.model.Alignment.FARM)
                        MeowfiaColors.Farm else MeowfiaColors.Meowfia,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = player.roleId.displayName,
                    color = MeowfiaColors.Primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                when (prompt) {
                    is NightPrompt.PickPlayer -> {
                        if (!hasConfirmed) {
                            Text(
                                text = prompt.instructionText,
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            var selectedTarget by remember { mutableStateOf<Int?>(null) }

                            PlayerPicker(
                                players = players,
                                excludePlayerId = if (prompt.excludeSelf) player.id else null,
                                selectedPlayerId = selectedTarget,
                                onPlayerSelected = { selectedTarget = it },
                                modifier = Modifier.weight(1f),
                                profileImages = GameSession.profileImages
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MeowfiaPrimaryButton(
                                text = "Confirm Visit",
                                onClick = {
                                    selectedTarget?.let { confirmedTarget = it }
                                },
                                enabled = selectedTarget != null
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                            val targetName = players.find { it.id == confirmedTarget }?.name ?: "?"
                            Text(
                                text = "Target confirmed: $targetName",
                                color = MeowfiaColors.Tertiary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }

                    is NightPrompt.Automatic -> {
                        Text(
                            text = prompt.instructionText,
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "Your action is automatic.",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    is NightPrompt.SelfVisit -> {
                        Text(
                            text = prompt.instructionText,
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "You visit yourself.",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
