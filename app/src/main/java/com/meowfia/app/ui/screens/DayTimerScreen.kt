package com.meowfia.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.bot.BotDayClaim
import com.meowfia.app.data.model.Player
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.components.CawCawButton
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.PhaseHeader
import com.meowfia.app.ui.components.PlayerPicker
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.components.TimerBar
import com.meowfia.app.ui.theme.MeowfiaColors
import kotlinx.coroutines.delay

private const val DAY_PHASE_SECONDS = 300 // 5 minutes
private const val MULBERRY_SECONDS = 30
private const val MULBERRY_AUTO_END_MS = 5000L

/** Represents an item in the day phase scrollable feed. */
private sealed class DayFeedItem {
    abstract val stableKey: String
    data class VisitGraphMessage(val lines: List<String>) : DayFeedItem() { override val stableKey = "visit-graph" }
    data class MeowfiaCountMessage(val count: Int) : DayFeedItem() { override val stableKey = "meowfia-count" }
    data class FlowerMessage(val flower: RoleId) : DayFeedItem() { override val stableKey = "flower-msg-${flower.name}" }
    data class FlowerAction(val flower: RoleId, val used: Boolean = false) : DayFeedItem() { override val stableKey = "flower-act-${flower.name}" }
    data class BotClaimItem(val claim: BotDayClaim) : DayFeedItem() { override val stableKey = "bot-claim-${claim.playerId}" }
    data class MulberryExtension(val clicked: Boolean = false) : DayFeedItem() { override val stableKey = "mulberry-ext" }
}

@Composable
fun DayTimerScreen(
    roundNumber: Int,
    cawCawCount: Int,
    activeFlowers: List<RoleId>,
    players: List<Player> = emptyList(),
    botClaims: List<BotDayClaim> = emptyList(),
    visitGraph: Map<Int, Int?> = emptyMap(),
    onCawCaw: () -> Unit,
    onTimeUp: () -> Unit,
    onSkipRound: (() -> Unit)? = null
) {
    // --- Dynamic start time (Mulberry) ---
    val isMulberry = RoleId.MULBERRY in activeFlowers
    val startingSeconds = if (isMulberry) MULBERRY_SECONDS else DAY_PHASE_SECONDS
    var remainingSeconds by remember { mutableIntStateOf(startingSeconds) }
    var timerPaused by remember { mutableStateOf(false) }

    // --- Bluebell: single CAW CAW threshold ---
    val cawCawThreshold = if (RoleId.BLUEBELL in activeFlowers) 1 else 3
    val isTimeUp = remainingSeconds <= 0 || cawCawCount >= cawCawThreshold

    // --- Mulberry extension state ---
    var mulberryExtensionOffered by remember { mutableStateOf(false) }
    var mulberryExtensionUsed by remember { mutableStateOf(false) }

    // --- Sunflower state ---
    var sunflowerUsed by remember { mutableStateOf(false) }
    var sunflowerStep by remember { mutableStateOf(SunflowerStep.IDLE) }
    var sunflowerUser by remember { mutableStateOf<Int?>(null) }
    var sunflowerTarget by remember { mutableStateOf<Int?>(null) }
    var sunflowerGuessRole by remember { mutableStateOf<RoleId?>(null) }
    var sunflowerResult by remember { mutableStateOf<String?>(null) }

    // --- Desert Pea state ---
    var desertPeaUsed by remember { mutableStateOf(false) }
    var desertPeaStep by remember { mutableStateOf(DesertPeaStep.IDLE) }
    var desertPeaUser by remember { mutableStateOf<Int?>(null) }
    var desertPeaGuesses by remember { mutableStateOf<Map<Int, RoleId>>(emptyMap()) }
    var desertPeaCurrentTarget by remember { mutableStateOf(0) } // index into targets list
    var desertPeaSelectedRole by remember { mutableStateOf<RoleId?>(null) }
    var desertPeaResult by remember { mutableStateOf<String?>(null) }

    // --- Cactus Flower: count Meowfia ---
    val meowfiaCount = if (RoleId.CACTUS_FLOWER in activeFlowers) {
        players.count { it.alignment == com.meowfia.app.data.model.Alignment.MEOWFIA }
    } else -1

    // --- Timer ---
    LaunchedEffect(timerPaused) {
        while (remainingSeconds > 0) {
            delay(1000)
            if (!timerPaused) remainingSeconds--
        }
    }

    // --- Mulberry: when timer hits 0, offer extension or auto-end after 5s ---
    if (isMulberry && remainingSeconds <= 0 && !mulberryExtensionOffered) {
        mulberryExtensionOffered = true
    }

    LaunchedEffect(mulberryExtensionOffered) {
        if (mulberryExtensionOffered && !mulberryExtensionUsed) {
            delay(MULBERRY_AUTO_END_MS)
            // If still not extended after 5 seconds, end the day
            if (!mulberryExtensionUsed && remainingSeconds <= 0) {
                onTimeUp()
            }
        }
    }

    // --- Build feed items ---
    val actionableFlowers = setOf(RoleId.SUNFLOWER, RoleId.DESERT_PEA)
    val feedItems = remember(
        activeFlowers, botClaims, sunflowerUsed, desertPeaUsed,
        visitGraph, meowfiaCount, mulberryExtensionOffered, mulberryExtensionUsed
    ) {
        val items = mutableListOf<DayFeedItem>()

        // Pitcher Plant: visit graph at top
        if (RoleId.PITCHER_PLANT in activeFlowers && visitGraph.isNotEmpty()) {
            val lines = visitGraph.mapNotNull { (visitorId, targetId) ->
                val visitor = players.find { it.id == visitorId }?.name ?: return@mapNotNull null
                if (targetId == null || targetId == visitorId) {
                    "$visitor stayed home"
                } else {
                    val target = players.find { it.id == targetId }?.name ?: "?"
                    "$visitor \u2192 $target"
                }
            }
            if (lines.isNotEmpty()) {
                items.add(DayFeedItem.VisitGraphMessage(lines))
            }
        }

        // Cactus Flower: Meowfia count
        if (RoleId.CACTUS_FLOWER in activeFlowers) {
            items.add(DayFeedItem.MeowfiaCountMessage(meowfiaCount))
        }

        // Flower cards
        for (flower in activeFlowers) {
            if (flower in actionableFlowers) {
                val used = when (flower) {
                    RoleId.SUNFLOWER -> sunflowerUsed
                    RoleId.DESERT_PEA -> desertPeaUsed
                    else -> false
                }
                items.add(DayFeedItem.FlowerAction(flower, used = used))
            } else {
                items.add(DayFeedItem.FlowerMessage(flower))
            }
        }

        // Mulberry extension button
        if (isMulberry && mulberryExtensionOffered && !mulberryExtensionUsed) {
            items.add(DayFeedItem.MulberryExtension(clicked = false))
        }

        // Bot claims
        for (claim in botClaims) {
            items.add(DayFeedItem.BotClaimItem(claim))
        }

        items.toList()
    }

    // --- Sunflower interaction overlay ---
    if (sunflowerStep != SunflowerStep.IDLE) {
        timerPaused = true
        SunflowerInteraction(
            step = sunflowerStep,
            players = players,
            selectedUser = sunflowerUser,
            selectedTarget = sunflowerTarget,
            selectedRole = sunflowerGuessRole,
            result = sunflowerResult,
            onSelectUser = { sunflowerUser = it },
            onConfirmUser = { sunflowerStep = SunflowerStep.PICK_TARGET },
            onSelectTarget = { sunflowerTarget = it },
            onConfirmTarget = { sunflowerStep = SunflowerStep.PICK_ROLE },
            onSelectRole = { sunflowerGuessRole = it },
            onConfirmRole = {
                val target = players.find { it.id == sunflowerTarget }
                val isMatch = target?.roleId == sunflowerGuessRole
                sunflowerResult = if (isMatch) {
                    "${target?.name} IS a ${sunflowerGuessRole?.displayName}!"
                } else {
                    "${target?.name} is NOT a ${sunflowerGuessRole?.displayName}."
                }
                sunflowerStep = SunflowerStep.SHOW_RESULT
            },
            onDone = {
                sunflowerUsed = true
                sunflowerStep = SunflowerStep.IDLE
                timerPaused = false
            }
        )
        return
    }

    // --- Desert Pea interaction overlay ---
    if (desertPeaStep != DesertPeaStep.IDLE) {
        timerPaused = true
        DesertPeaInteraction(
            step = desertPeaStep,
            players = players,
            userId = desertPeaUser,
            currentTargetIndex = desertPeaCurrentTarget,
            selectedRole = desertPeaSelectedRole,
            guesses = desertPeaGuesses,
            result = desertPeaResult,
            onSelectUser = { desertPeaUser = it },
            onConfirmUser = {
                desertPeaCurrentTarget = 0
                desertPeaGuesses = emptyMap()
                desertPeaStep = DesertPeaStep.GUESS_ROLE
            },
            onSelectRole = { desertPeaSelectedRole = it },
            onConfirmGuess = {
                val targets = players.filter { it.id != desertPeaUser }
                val targetPlayer = targets[desertPeaCurrentTarget]
                val updatedGuesses = desertPeaGuesses + (targetPlayer.id to desertPeaSelectedRole!!)
                desertPeaGuesses = updatedGuesses
                desertPeaSelectedRole = null

                if (desertPeaCurrentTarget + 1 < targets.size) {
                    desertPeaCurrentTarget++
                } else {
                    // All guesses in — resolve
                    var correct = 0
                    val total = updatedGuesses.size
                    for ((pid, guessedRole) in updatedGuesses) {
                        val actual = players.find { it.id == pid }?.roleId
                        if (actual == guessedRole) correct++
                    }
                    val allCorrect = correct == total
                    val userName = players.find { it.id == desertPeaUser }?.name ?: "?"
                    desertPeaResult = if (allCorrect) {
                        "ALL CORRECT! $userName wins exclusively!"
                    } else {
                        "$correct/$total correct. WRONG! $userName has been killed."
                    }
                    desertPeaStep = DesertPeaStep.SHOW_RESULT
                }
            },
            onDone = {
                desertPeaUsed = true
                desertPeaStep = DesertPeaStep.IDLE
                timerPaused = false
            }
        )
        return
    }

    // --- Main day phase UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhaseHeader(roundNumber = roundNumber, phaseName = "Day Phase")
        Spacer(modifier = Modifier.height(12.dp))

        TimerBar(
            remainingSeconds = remainingSeconds,
            totalSeconds = startingSeconds
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable feed
        if (feedItems.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(feedItems, key = { it.stableKey }) { item ->
                    when (item) {
                        is DayFeedItem.VisitGraphMessage -> VisitGraphCard(item.lines)
                        is DayFeedItem.MeowfiaCountMessage -> MeowfiaCountCard(
                            count = item.count,
                            onSkipRound = onSkipRound
                        )
                        is DayFeedItem.FlowerMessage -> FlowerMessageCard(item.flower)
                        is DayFeedItem.FlowerAction -> FlowerActionCard(
                            flower = item.flower,
                            used = item.used,
                            onActivate = {
                                when (item.flower) {
                                    RoleId.SUNFLOWER -> if (!sunflowerUsed) {
                                        sunflowerStep = SunflowerStep.PICK_USER
                                    }
                                    RoleId.DESERT_PEA -> if (!desertPeaUsed) {
                                        desertPeaStep = DesertPeaStep.PICK_USER
                                    }
                                    else -> {}
                                }
                            }
                        )
                        is DayFeedItem.BotClaimItem -> BotClaimCard(
                            claim = item.claim,
                            profileImage = com.meowfia.app.engine.GameSession.profileImages[item.claim.playerId]
                        )
                        is DayFeedItem.MulberryExtension -> MulberryExtensionCard(
                            onBuyTime = {
                                mulberryExtensionUsed = true
                                mulberryExtensionOffered = false
                                remainingSeconds += MULBERRY_SECONDS
                            }
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isTimeUp && !(isMulberry && mulberryExtensionOffered && !mulberryExtensionUsed)) {
            val cawText = if (RoleId.BLUEBELL in activeFlowers && cawCawCount >= 1) {
                "CAW CAW \u2014 BLUEBELL ENDS THE DAY!"
            } else if (cawCawCount >= cawCawThreshold) {
                "CAW CAW \u2014 EARLY VOTE!"
            } else {
                "TIME'S UP \u2014 VOTE!"
            }
            Text(
                text = cawText,
                color = MeowfiaColors.Secondary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            MeowfiaPrimaryButton(text = "Begin Voting", onClick = onTimeUp)
        } else {
            CawCawButton(cawCount = cawCawCount, onCawCaw = onCawCaw)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// --- Feed Card Composables ---

@Composable
private fun VisitGraphCard(lines: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(1.5.dp, MeowfiaColors.Secondary.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoleIcon(roleId = RoleId.PITCHER_PLANT, size = 28)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pitcher Plant \u2014 Visit Graph",
                    color = MeowfiaColors.Secondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            for (line in lines) {
                Text(
                    text = line,
                    color = MeowfiaColors.TextPrimary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun MeowfiaCountCard(count: Int, onSkipRound: (() -> Unit)?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (count == 0) MeowfiaColors.Primary.copy(alpha = 0.1f)
            else MeowfiaColors.SurfaceElevated
        ),
        border = BorderStroke(
            1.5.dp,
            if (count == 0) MeowfiaColors.Primary.copy(alpha = 0.6f)
            else MeowfiaColors.Confused.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoleIcon(roleId = RoleId.CACTUS_FLOWER, size = 28)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cactus Flower",
                    color = MeowfiaColors.Confused,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            if (count == 0) {
                Text(
                    text = "No Meowfia this round! Round skipped \u2014 no discussion, no vote, no eggs lost.",
                    color = MeowfiaColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (onSkipRound != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    MeowfiaPrimaryButton(text = "Skip Round", onClick = onSkipRound)
                }
            } else {
                Text(
                    text = "There are $count Meowfia player${if (count != 1) "s" else ""} this round.",
                    color = MeowfiaColors.TextPrimary,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun MulberryExtensionCard(onBuyTime: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.Primary.copy(alpha = 0.1f)),
        border = BorderStroke(2.dp, MeowfiaColors.Primary.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoleIcon(roleId = RoleId.MULBERRY, size = 28)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Time's up!",
                    color = MeowfiaColors.Primary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Spend 1 egg to buy 30 more seconds. Auto-ends in 5 seconds if nobody acts.",
                color = MeowfiaColors.TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            MeowfiaPrimaryButton(text = "Buy 30 Seconds (1 Egg)", onClick = onBuyTime)
        }
    }
}

@Composable
private fun FlowerMessageCard(flower: RoleId) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(1.dp, MeowfiaColors.Confused.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoleIcon(roleId = flower, size = 32)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flower.displayName,
                    color = MeowfiaColors.Confused,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (flower) {
                        RoleId.BLUEBELL -> "Bluebell: One CAW CAW ends the day! The caller gains an egg."
                        RoleId.NIGHTSHADE -> "Next round will have a Dusk phase before Night."
                        else -> flower.description
                    },
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun FlowerActionCard(flower: RoleId, used: Boolean, onActivate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (used) MeowfiaColors.Surface else MeowfiaColors.SurfaceElevated
        ),
        border = BorderStroke(
            1.dp,
            if (used) MeowfiaColors.Dead.copy(alpha = 0.3f) else MeowfiaColors.Primary.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoleIcon(roleId = flower, size = 32)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flower.displayName,
                    color = if (used) MeowfiaColors.Dead else MeowfiaColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (used) "Used" else flower.description,
                    color = if (used) MeowfiaColors.Dead else MeowfiaColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (!used) {
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onActivate,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, MeowfiaColors.Primary),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text("Use", color = MeowfiaColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun BotClaimCard(claim: BotDayClaim, profileImage: android.graphics.Bitmap?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MeowfiaColors.SurfaceElevated),
        border = BorderStroke(1.dp, MeowfiaColors.Primary.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.meowfia.app.ui.components.ProfileThumbnail(
                bitmap = profileImage,
                size = 32
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = claim.botName,
                    color = MeowfiaColors.Primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = claim.toDisplayText(),
                    color = MeowfiaColors.TextPrimary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// --- Sunflower Interaction ---

private enum class SunflowerStep { IDLE, PICK_USER, PICK_TARGET, PICK_ROLE, SHOW_RESULT }

@Composable
private fun SunflowerInteraction(
    step: SunflowerStep,
    players: List<Player>,
    selectedUser: Int?,
    selectedTarget: Int?,
    selectedRole: RoleId?,
    result: String?,
    onSelectUser: (Int) -> Unit,
    onConfirmUser: () -> Unit,
    onSelectTarget: (Int) -> Unit,
    onConfirmTarget: () -> Unit,
    onSelectRole: (RoleId) -> Unit,
    onConfirmRole: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoleIcon(roleId = RoleId.SUNFLOWER, size = 48)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sunflower",
            color = MeowfiaColors.Primary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Spend one egg to learn if a player is a certain animal.",
            color = MeowfiaColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (step) {
            SunflowerStep.PICK_USER -> {
                Text("Who is using the Sunflower?", color = MeowfiaColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                PlayerPicker(
                    players = players.filter { !it.isBot },
                    selectedPlayerId = selectedUser,
                    onPlayerSelected = onSelectUser,
                    modifier = Modifier.weight(1f),
                    profileImages = com.meowfia.app.engine.GameSession.profileImages
                )
                MeowfiaPrimaryButton(
                    text = "Confirm",
                    onClick = onConfirmUser,
                    enabled = selectedUser != null
                )
            }
            SunflowerStep.PICK_TARGET -> {
                Text("Who do you want to investigate?", color = MeowfiaColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                PlayerPicker(
                    players = players.filter { it.id != selectedUser },
                    selectedPlayerId = selectedTarget,
                    onPlayerSelected = onSelectTarget,
                    modifier = Modifier.weight(1f),
                    profileImages = com.meowfia.app.engine.GameSession.profileImages
                )
                MeowfiaPrimaryButton(
                    text = "Confirm",
                    onClick = onConfirmTarget,
                    enabled = selectedTarget != null
                )
            }
            SunflowerStep.PICK_ROLE -> {
                val targetName = players.find { it.id == selectedTarget }?.name ?: "?"
                Text("Is $targetName a...", color = MeowfiaColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                RolePicker(
                    selectedRole = selectedRole,
                    onSelectRole = onSelectRole,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                MeowfiaPrimaryButton(
                    text = "Check",
                    onClick = onConfirmRole,
                    enabled = selectedRole != null
                )
            }
            SunflowerStep.SHOW_RESULT -> {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = result ?: "",
                    color = MeowfiaColors.Primary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Show this to the player who used the Sunflower, then dismiss.",
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                MeowfiaPrimaryButton(text = "Done", onClick = onDone)
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// --- Desert Pea Interaction ---

private enum class DesertPeaStep { IDLE, PICK_USER, GUESS_ROLE, SHOW_RESULT }

@Composable
private fun DesertPeaInteraction(
    step: DesertPeaStep,
    players: List<Player>,
    userId: Int?,
    currentTargetIndex: Int,
    selectedRole: RoleId?,
    guesses: Map<Int, RoleId>,
    result: String?,
    onSelectUser: (Int) -> Unit,
    onConfirmUser: () -> Unit,
    onSelectRole: (RoleId) -> Unit,
    onConfirmGuess: () -> Unit,
    onDone: () -> Unit
) {
    val targets = remember(userId, players) {
        players.filter { it.id != userId }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RoleIcon(roleId = RoleId.DESERT_PEA, size = 48)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Desert Pea",
            color = MeowfiaColors.Primary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Guess every player's animal. All correct = exclusive win. Any wrong = death.",
            color = MeowfiaColors.TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        when (step) {
            DesertPeaStep.PICK_USER -> {
                Text("Who is using the Desert Pea?", color = MeowfiaColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                PlayerPicker(
                    players = players.filter { !it.isBot },
                    selectedPlayerId = userId,
                    onPlayerSelected = onSelectUser,
                    modifier = Modifier.weight(1f),
                    profileImages = com.meowfia.app.engine.GameSession.profileImages
                )
                MeowfiaPrimaryButton(
                    text = "Confirm",
                    onClick = onConfirmUser,
                    enabled = userId != null
                )
            }
            DesertPeaStep.GUESS_ROLE -> {
                if (currentTargetIndex < targets.size) {
                    val targetPlayer = targets[currentTargetIndex]
                    Text(
                        text = "What animal is ${targetPlayer.name}?",
                        color = MeowfiaColors.TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Player ${currentTargetIndex + 1} of ${targets.size}",
                        color = MeowfiaColors.TextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    RolePicker(
                        selectedRole = selectedRole,
                        onSelectRole = onSelectRole,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MeowfiaPrimaryButton(
                        text = if (currentTargetIndex + 1 < targets.size) "Next" else "Submit All",
                        onClick = onConfirmGuess,
                        enabled = selectedRole != null
                    )
                }
            }
            DesertPeaStep.SHOW_RESULT -> {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = result ?: "",
                    color = MeowfiaColors.Primary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Show this result, then dismiss.",
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.weight(1f))
                MeowfiaPrimaryButton(text = "Done", onClick = onDone)
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

// --- Shared Role Picker ---

@Composable
private fun RolePicker(
    selectedRole: RoleId?,
    onSelectRole: (RoleId) -> Unit,
    modifier: Modifier = Modifier
) {
    val implementedRoles = remember {
        RoleId.entries.filter { it.implemented && (it.isFarmAnimal || it.isMeowfiaAnimal) }
    }
    LazyColumn(modifier = modifier) {
        items(implementedRoles, key = { it.name }) { role ->
            val isSelected = role == selectedRole
            Card(
                onClick = { onSelectRole(role) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MeowfiaColors.Primary.copy(alpha = 0.15f)
                    else MeowfiaColors.Surface
                ),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MeowfiaColors.Primary else MeowfiaColors.TextSecondary.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoleIcon(roleId = role, size = 28)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = role.displayName,
                        color = if (isSelected) MeowfiaColors.Primary else MeowfiaColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
