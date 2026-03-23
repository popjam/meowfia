package com.meowfia.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.NightActionEntry
import com.meowfia.app.data.model.PostRoundAnalysis
import com.meowfia.app.data.model.StatusEffect
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun PostRoundAnalysisScreen(
    analysis: PostRoundAnalysis,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Round ${analysis.roundNumber} Analysis",
            color = MeowfiaColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Detailed walkthrough of the night",
            color = MeowfiaColors.TextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {

            // --- Section 1: Pool ---
            item { SectionHeader("The Pool") }
            item {
                val rolesText = analysis.poolSummary.roles.joinToString(", ")
                val flowersText = if (analysis.poolSummary.flowers.isNotEmpty())
                    analysis.poolSummary.flowers.joinToString(", ")
                else "None"
                InfoCard {
                    LabelValue("Roles", rolesText)
                    Spacer(modifier = Modifier.height(4.dp))
                    LabelValue("Flowers", flowersText)
                }
            }

            // --- Section 2: Active Flowers ---
            if (analysis.activeFlowers.isNotEmpty()) {
                item { SectionHeader("Active Flowers") }
                items(analysis.activeFlowers) { flower ->
                    InfoCard {
                        Text(
                            text = flower.name,
                            color = MeowfiaColors.Primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = flower.description,
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // --- Section 3: Assignments ---
            item { SectionHeader("Player Assignments") }
            items(analysis.playerAssignments) { assignment ->
                val alignmentColor = if (assignment.alignment == com.meowfia.app.data.model.Alignment.FARM)
                    MeowfiaColors.Farm else MeowfiaColors.Meowfia
                val botTag = if (assignment.isBot) " [BOT]" else ""
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Alignment dot
                        Spacer(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(alignmentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${assignment.playerName}$botTag",
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = assignment.alignment.displayName,
                            color = alignmentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${assignment.roleName} — ${assignment.roleDescription}",
                        color = MeowfiaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            // --- Section 4: Resolution Order ---
            item { SectionHeader("Night Resolution Order") }
            item {
                val useDandelion = analysis.activeFlowers.any { it.name == "Dandelion" }
                InfoCard {
                    if (useDandelion) {
                        Text(
                            text = "Dandelion active — resolved in seat order",
                            color = MeowfiaColors.Confused,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    analysis.resolutionOrder.forEachIndexed { index, entry ->
                        Text(
                            text = "${index + 1}. $entry",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // --- Section 5: Night Walkthrough ---
            item { SectionHeader("Night Walkthrough") }
            itemsIndexed(analysis.nightWalkthrough) { index, entry ->
                NightActionCard(index + 1, entry)
            }

            // --- Section 6: Visit Map ---
            item { SectionHeader("Visit Map") }
            item {
                InfoCard {
                    analysis.visitMap.forEach { visit ->
                        val targetText = visit.targetName ?: "nobody"
                        Text(
                            text = "${visit.visitorName} -> $targetText",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // --- Section 7: Egg Summary ---
            if (analysis.eggSummary.isNotEmpty()) {
                item { SectionHeader("Egg Changes") }
                items(analysis.eggSummary) { entry ->
                    val deltaColor = when {
                        entry.delta > 0 -> MeowfiaColors.Farm
                        entry.delta < 0 -> MeowfiaColors.Secondary
                        else -> MeowfiaColors.TextSecondary
                    }
                    val sign = if (entry.delta > 0) "+" else ""
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.playerName,
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$sign${entry.delta}",
                                color = deltaColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (entry.breakdown.isNotEmpty()) {
                            Text(
                                text = entry.breakdown,
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // --- Section 8: Role Changes ---
            if (analysis.roleChanges.isNotEmpty()) {
                item { SectionHeader("Role Changes") }
                items(analysis.roleChanges) { change ->
                    InfoCard {
                        Text(
                            text = change.playerName,
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${change.fromRole} -> ${change.toRole}",
                            color = MeowfiaColors.Primary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = change.cause,
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // --- Section 9: Alignment Changes ---
            if (analysis.alignmentChanges.isNotEmpty()) {
                item { SectionHeader("Alignment Changes") }
                items(analysis.alignmentChanges) { change ->
                    val fromColor = if (change.fromAlignment == com.meowfia.app.data.model.Alignment.FARM)
                        MeowfiaColors.Farm else MeowfiaColors.Meowfia
                    val toColor = if (change.toAlignment == com.meowfia.app.data.model.Alignment.FARM)
                        MeowfiaColors.Farm else MeowfiaColors.Meowfia
                    InfoCard {
                        Text(
                            text = change.playerName,
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row {
                            Text(
                                text = change.fromAlignment.displayName,
                                color = fromColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " -> ",
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = change.toAlignment.displayName,
                                color = toColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = change.cause,
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 12.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // --- Section 10: Status Effects ---
            if (analysis.statusEffects.isNotEmpty()) {
                item { SectionHeader("Status Effects Applied") }
                items(analysis.statusEffects) { entry ->
                    val effectColor = when (entry.effect) {
                        StatusEffect.CONFUSED -> MeowfiaColors.Confused
                        StatusEffect.HUGGED -> MeowfiaColors.Hugged
                        StatusEffect.DEAD -> MeowfiaColors.Dead
                        StatusEffect.HAS_WINK -> MeowfiaColors.Primary
                    }
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.playerName,
                                color = MeowfiaColors.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = entry.effect.name,
                                color = effectColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- Section 11: Elimination & Outcome ---
            item { SectionHeader("Outcome") }
            item {
                InfoCard {
                    val elim = analysis.eliminationSummary
                    if (elim != null) {
                        val alignmentColor = if (elim.alignment == com.meowfia.app.data.model.Alignment.FARM)
                            MeowfiaColors.Farm else MeowfiaColors.Meowfia
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(elim.playerName)
                                }
                                append(" was eggsecuted. They were a ")
                                withStyle(SpanStyle(color = alignmentColor, fontWeight = FontWeight.Bold)) {
                                    append("${elim.alignment.displayName} ${elim.roleName}")
                                }
                                append(".")
                            },
                            color = MeowfiaColors.TextPrimary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (elim.wasCorrectElimination)
                                "Correct elimination — the group found a Meowfia player."
                            else
                                "Wrong target — an innocent Farm player was eliminated.",
                            color = if (elim.wasCorrectElimination) MeowfiaColors.Farm else MeowfiaColors.Secondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "No player was eliminated this round.",
                            color = MeowfiaColors.TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val winTeam = analysis.winningTeam
                    if (winTeam != null) {
                        val winColor = if (winTeam == com.meowfia.app.data.model.Alignment.FARM)
                            MeowfiaColors.Farm else MeowfiaColors.Meowfia
                        Text(
                            text = "${winTeam.displayName} wins!",
                            color = winColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- Section 12: Narrative Log ---
            if (analysis.narrativeLog.isNotEmpty()) {
                item { SectionHeader("Engine Narrative Log") }
                item {
                    InfoCard {
                        analysis.narrativeLog.forEach { line ->
                            Text(
                                text = line,
                                color = MeowfiaColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        Spacer(modifier = Modifier.height(8.dp))
        MeowfiaPrimaryButton(text = "Back", onClick = onBack)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = title.uppercase(),
            color = MeowfiaColors.Primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        HorizontalDivider(
            color = MeowfiaColors.Primary.copy(alpha = 0.3f),
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
private fun NightActionCard(step: Int, entry: NightActionEntry) {
    val alignmentColor = if (entry.alignment == com.meowfia.app.data.model.Alignment.FARM)
        MeowfiaColors.Farm else MeowfiaColors.Meowfia

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(1.dp, alignmentColor.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$step.",
                        color = MeowfiaColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.playerName,
                        color = MeowfiaColors.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.roleName,
                        color = MeowfiaColors.Primary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Spacer(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(alignmentColor)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))

            val targetText = if (entry.targetName != null) "${entry.action} ${entry.targetName}"
            else entry.action
            Text(
                text = targetText,
                color = MeowfiaColors.TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.outcome,
                color = MeowfiaColors.TextPrimary,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = MeowfiaColors.TextSecondary, fontWeight = FontWeight.SemiBold)) {
                append("$label: ")
            }
            withStyle(SpanStyle(color = MeowfiaColors.TextPrimary)) {
                append(value)
            }
        },
        fontSize = 14.sp
    )
}
