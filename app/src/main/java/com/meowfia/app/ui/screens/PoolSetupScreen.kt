package com.meowfia.app.ui.screens

import android.media.ToneGenerator
import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.qr.QrScannerView
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.RoleCardOverlay
import com.meowfia.app.ui.components.RoleGrid
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.theme.MeowfiaColors
import com.meowfia.app.util.hapticTick
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoolSetupScreen(
    onStartGame: (selectedRoles: List<RoleId>, playerCount: Int, botCount: Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = QR (default), 1 = Manual
    var playerCount by remember { mutableIntStateOf(6) }
    var botCount by remember { mutableIntStateOf(0) }
    val selectedRoles = remember {
        mutableStateListOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
    }
    var overlayRole by remember { mutableStateOf<RoleId?>(null) }
    var lastScannedRole by remember { mutableStateOf<RoleId?>(null) }
    val view = LocalView.current

    // Confirmation sound generator
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }
    DisposableEffect(Unit) {
        onDispose { toneGenerator.release() }
    }

    // Auto-clear scanned confirmation after 1.5s
    LaunchedEffect(lastScannedRole) {
        if (lastScannedRole != null) {
            delay(1500)
            lastScannedRole = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Pool Setup",
                color = MeowfiaColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Player count selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Players:", color = MeowfiaColors.TextPrimary, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeowfiaSecondaryButton(
                        text = "-",
                        onClick = {
                            if (playerCount > 3) {
                                playerCount--
                                if (botCount > playerCount) botCount = playerCount
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = playerCount > 3
                    )
                    Text(
                        text = "$playerCount",
                        color = MeowfiaColors.Primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    MeowfiaSecondaryButton(
                        text = "+",
                        onClick = { if (playerCount < 8) playerCount++ },
                        modifier = Modifier.weight(1f),
                        enabled = playerCount < 8
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bot count selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Bots:", color = MeowfiaColors.TextPrimary, fontSize = 18.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MeowfiaSecondaryButton(
                        text = "-",
                        onClick = { if (botCount > 0) botCount-- },
                        modifier = Modifier.weight(1f),
                        enabled = botCount > 0
                    )
                    Text(
                        text = "$botCount",
                        color = MeowfiaColors.Primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    MeowfiaSecondaryButton(
                        text = "+",
                        onClick = { if (botCount < playerCount) botCount++ },
                        modifier = Modifier.weight(1f),
                        enabled = botCount < playerCount
                    )
                }
            }

            if (botCount == playerCount) {
                Text(
                    text = "Spectator mode — all players are bots",
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pool bubbles - horizontal scrollable row
            PoolBubbleRow(
                roles = selectedRoles,
                onRemove = { role -> selectedRoles.remove(role) },
                onLongPress = { role -> overlayRole = role }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tab selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MeowfiaColors.Surface
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Scan QR", modifier = Modifier.padding(12.dp), color = MeowfiaColors.TextPrimary)
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Manual", modifier = Modifier.padding(12.dp), color = MeowfiaColors.TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> {
                    // QR scanner with front camera
                    Box(modifier = Modifier.weight(1f)) {
                        QrScannerView(
                            onCardScanned = { roleId ->
                                selectedRoles.add(roleId)
                                lastScannedRole = roleId
                                toneGenerator.startTone(ToneGenerator.TONE_PROP_ACK, 150)
                                view.hapticTick()
                            },
                            useFrontCamera = true,
                            allowDuplicates = true,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                        )

                        // Scan confirmation overlay
                        AnimatedVisibility(
                            visible = lastScannedRole != null,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            lastScannedRole?.let { scanned ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MeowfiaColors.Surface.copy(alpha = 0.9f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        RoleIcon(roleId = scanned, size = 56)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = scanned.displayName,
                                            color = MeowfiaColors.Primary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Added!",
                                            color = MeowfiaColors.Farm,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Manual selection — role grid
                    val implementedRoles = remember {
                        RoleId.entries.filter { it.implemented && !it.isBuffer }
                    }

                    RoleGrid(
                        roles = implementedRoles,
                        onRoleTap = { role -> selectedRoles.add(role) },
                        onRoleLongPress = { role -> overlayRole = role },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeowfiaSecondaryButton(
                    text = "All Roles",
                    onClick = {
                        val allImplemented = RoleId.entries.filter { it.implemented && !it.isBuffer }
                        for (role in allImplemented) {
                            if (role !in selectedRoles) selectedRoles.add(role)
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp)
                )
                MeowfiaSecondaryButton(
                    text = "Clear",
                    onClick = {
                        selectedRoles.removeAll { !it.isBuffer }
                    },
                    modifier = Modifier.weight(1f).height(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (selectedRoles.size > 2) {
                MeowfiaPrimaryButton(
                    text = "Start Game",
                    onClick = { onStartGame(selectedRoles.toList(), playerCount, botCount) }
                )
            } else {
                MeowfiaPrimaryButton(
                    text = "Start with Defaults",
                    onClick = {
                        val defaults = listOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
                        onStartGame(defaults, playerCount, botCount)
                    }
                )
            }
        }

        // Role card overlay (long-press)
        RoleCardOverlay(roleId = overlayRole, onDismiss = { overlayRole = null })
    }
}

/**
 * Horizontal scrollable row of pool "bubbles" showing selected roles.
 * Duplicate roles are stacked with a count badge.
 * Buffer roles (Pigeon, House Cat) have a dashed-style indicator.
 * Tapping removes one copy; long-press shows the role card.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PoolBubbleRow(
    roles: List<RoleId>,
    onRemove: (RoleId) -> Unit,
    onLongPress: (RoleId) -> Unit
) {
    // Group by role and count
    val grouped = remember(roles.toList()) {
        val counts = mutableMapOf<RoleId, Int>()
        for (r in roles) counts[r] = (counts[r] ?: 0) + 1
        // Preserve insertion order of first occurrence
        val seen = mutableSetOf<RoleId>()
        roles.mapNotNull { r ->
            if (seen.add(r)) r to counts[r]!! else null
        }
    }

    Column {
        Text(
            text = "Pool: ${roles.size} cards",
            color = MeowfiaColors.TextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(grouped, key = { it.first.name }) { (role, count) ->
                Box(
                    modifier = Modifier
                        .combinedClickable(
                            onClick = { if (!role.isBuffer) onRemove(role) },
                            onLongClick = { onLongPress(role) }
                        )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(56.dp)
                    ) {
                        Box {
                            RoleIcon(roleId = role, size = 44)

                            // Count badge for duplicates
                            if (count > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(20.dp)
                                        .background(MeowfiaColors.Secondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$count",
                                        color = MeowfiaColors.TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Buffer indicator
                            if (role.isBuffer) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 2.dp, y = 2.dp)
                                        .size(14.dp)
                                        .background(MeowfiaColors.Primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "B",
                                        color = MeowfiaColors.TextOnPrimary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = role.displayName,
                            color = if (role.isBuffer) MeowfiaColors.Primary else MeowfiaColors.TextPrimary,
                            fontSize = 10.sp,
                            fontStyle = if (role.isBuffer) FontStyle.Italic else FontStyle.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}
