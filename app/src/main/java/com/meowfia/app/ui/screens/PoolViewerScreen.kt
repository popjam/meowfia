package com.meowfia.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.RoleCardOverlay
import com.meowfia.app.ui.components.RoleGrid
import com.meowfia.app.ui.theme.MeowfiaColors

private data class EffectInfo(
    val effect: StatusEffect,
    val displayName: String,
    val initial: String,
    val color: Color,
    val description: String
)

private val EFFECTS = listOf(
    EffectInfo(
        StatusEffect.CONFUSED, "Confusion", "C", MeowfiaColors.Confused,
        "A confused player receives incorrect information in their dawn report. " +
            "This can include a wrong egg count or other misleading details. " +
            "A confused player must still act on the information they are given — " +
            "for example, drawing or discarding cards based on their (incorrect) reported nest count."
    ),
    EffectInfo(
        StatusEffect.HUGGED, "Hug (Roleblock)", "H", MeowfiaColors.Hugged,
        "When a role says it \"hugs\" a player, that player is roleblocked. " +
            "Their visit still goes through (they still visit their chosen target), " +
            "but all of their role's passive and active effects are cancelled. " +
            "For example, a hugged Pigeon will visit their target but will not lay an egg."
    ),
    EffectInfo(
        StatusEffect.HAS_WINK, "Wink", "W", MeowfiaColors.Primary,
        "Some roles grant a player a Wink. During the day phase, a player holding " +
            "a Wink may secretly wink at another player. That player must cease talking " +
            "and die 5–10 seconds later. A Wink is a one-use ability — once used, it is spent. " +
            "The wink must be subtle; if others notice, that's part of the game."
    ),
    EffectInfo(
        StatusEffect.DEAD, "Death", "D", MeowfiaColors.Dead,
        "When a player dies (from a Wink, Stinging Bush, or any other effect), " +
            "they must immediately collapse and act out a dramatic death. " +
            "Dead players cannot talk for the remainder of the day phase. " +
            "Dead players can still throw eggs during eggsecution, but their thrown cards " +
            "do not count as votes. Dead players' thrown cards are still resolved normally for scoring."
    )
)

@Composable
fun PoolViewerScreen(
    onBack: () -> Unit
) {
    var overlayRole by remember { mutableStateOf<RoleId?>(null) }
    var overlayEffect by remember { mutableStateOf<EffectInfo?>(null) }

    val roles = remember { RoleId.entries.filter { it.implemented } }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Role Reference",
                color = MeowfiaColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            RoleGrid(
                roles = roles,
                onRoleTap = { role -> overlayRole = role },
                onRoleLongPress = { role -> overlayRole = role },
                modifier = Modifier.weight(1f)
            )

            // Effects section
            Spacer(modifier = Modifier.height(12.dp))
            Text("Effects", color = MeowfiaColors.Primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (effect in EFFECTS) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f).clickable { overlayEffect = effect }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(effect.color)
                                .border(1.5.dp, effect.color, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                effect.initial,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            effect.displayName,
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            MeowfiaSecondaryButton(text = "Back", onClick = onBack)
        }

        // Role overlay
        RoleCardOverlay(roleId = overlayRole, onDismiss = { overlayRole = null })

        // Effect overlay
        if (overlayEffect != null) {
            val eff = overlayEffect!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { overlayEffect = null },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {},
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MeowfiaColors.Surface),
                    border = androidx.compose.foundation.BorderStroke(3.dp, eff.color)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(eff.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(eff.initial, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            eff.displayName,
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Effect", color = eff.color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            eff.description,
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }
        }
    }
}
