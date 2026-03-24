package com.meowfia.app.ui.screens

import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.meowfia.app.data.model.PreConfiguredPlayer
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.qr.QrScannerView
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.ProfilePicturePicker
import com.meowfia.app.ui.components.ProfileThumbnail
import com.meowfia.app.ui.components.RoleCardOverlay
import com.meowfia.app.ui.components.RoleGrid
import com.meowfia.app.ui.components.RoleIcon
import com.meowfia.app.ui.components.generateColorProfile
import com.meowfia.app.ui.theme.MeowfiaColors
import com.meowfia.app.util.hapticTick
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoolSetupScreen(
    onStartGame: (selectedRoles: List<RoleId>, playerSlots: List<PreConfiguredPlayer>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val selectedRoles = remember { mutableStateListOf(RoleId.PIGEON, RoleId.HOUSE_CAT) }
    val playerSlots = remember {
        mutableStateListOf<PreConfiguredPlayer>().apply {
            repeat(3) { add(PreConfiguredPlayer(isBot = true)) }
        }
    }
    var editingPlayerIndex by remember { mutableStateOf<Int?>(null) }
    var overlayRole by remember { mutableStateOf<RoleId?>(null) }
    var lastScannedRole by remember { mutableStateOf<RoleId?>(null) }
    val view = LocalView.current

    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80) }
    DisposableEffect(Unit) { onDispose { toneGenerator.release() } }

    LaunchedEffect(lastScannedRole) {
        if (lastScannedRole != null) { delay(1500); lastScannedRole = null }
    }

    val playerCount = playerSlots.size
    val botCount = playerSlots.count { it.isBot }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text("Pool Setup", color = MeowfiaColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // Player bubble row
            Text("Players", color = MeowfiaColors.TextSecondary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            PlayerBubbleRow(
                slots = playerSlots,
                onRemove = { index -> if (playerSlots.size > 3) playerSlots.removeAt(index) },
                onAdd = { if (playerSlots.size < 8) playerSlots.add(PreConfiguredPlayer()) },
                onLongPress = { index -> editingPlayerIndex = index }
            )

            if (playerSlots.all { it.isBot }) {
                Text("Spectator mode", color = MeowfiaColors.TextSecondary, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pool bubbles
            PoolBubbleRow(
                roles = selectedRoles,
                onRemove = { role -> selectedRoles.remove(role) },
                onLongPress = { role -> overlayRole = role }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Tab selector
            TabRow(selectedTabIndex = selectedTab, containerColor = MeowfiaColors.Surface) {
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
                        androidx.compose.animation.AnimatedVisibility(
                            visible = lastScannedRole != null,
                            enter = androidx.compose.animation.scaleIn() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            lastScannedRole?.let { scanned ->
                                Box(
                                    modifier = Modifier
                                        .background(MeowfiaColors.Surface.copy(alpha = 0.9f), RoundedCornerShape(16.dp))
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        RoleIcon(roleId = scanned, size = 56)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(scanned.displayName, color = MeowfiaColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        Text("Added!", color = MeowfiaColors.Farm, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    val implementedRoles = remember { RoleId.entries.filter { it.implemented && !it.isBuffer } }
                    RoleGrid(
                        roles = implementedRoles,
                        selectedRoles = selectedRoles.toSet(),
                        onRoleTap = { role ->
                            if (role in selectedRoles) selectedRoles.remove(role)
                            else selectedRoles.add(role)
                        },
                        onRoleLongPress = { role -> overlayRole = role },
                        onCategoryTap = { type ->
                            val rolesOfType = implementedRoles.filter { it.cardType == type }
                            val allSelected = rolesOfType.all { it in selectedRoles }
                            if (allSelected) {
                                selectedRoles.removeAll { it.cardType == type && !it.isBuffer }
                            } else {
                                for (r in rolesOfType) { if (r !in selectedRoles) selectedRoles.add(r) }
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            if (selectedRoles.size > 2) {
                MeowfiaPrimaryButton(
                    text = "Start Game",
                    onClick = { onStartGame(selectedRoles.toList(), playerSlots.toList()) }
                )
            } else {
                MeowfiaPrimaryButton(
                    text = "Start with Defaults",
                    onClick = {
                        onStartGame(listOf(RoleId.PIGEON, RoleId.HOUSE_CAT), playerSlots.toList())
                    }
                )
            }
        }

        // Role card overlay
        RoleCardOverlay(roleId = overlayRole, onDismiss = { overlayRole = null })

        // Player customization overlay
        if (editingPlayerIndex != null) {
            val idx = editingPlayerIndex!!
            PlayerCustomizationOverlay(
                slot = playerSlots[idx],
                playerIndex = idx,
                onUpdate = { updated -> playerSlots[idx] = updated },
                onDismiss = { editingPlayerIndex = null }
            )
        }
    }
}

// --- Player Bubble Row ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayerBubbleRow(
    slots: List<PreConfiguredPlayer>,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    onLongPress: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(slots) { index, slot ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(52.dp)
                    .height(66.dp)
                    .combinedClickable(
                        onClick = { onRemove(index) },
                        onLongClick = { onLongPress(index) }
                    )
            ) {
                if (slot.isBot) {
                    // Bot bubble
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MeowfiaColors.SurfaceElevated)
                            .border(2.dp, MeowfiaColors.TextSecondary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("B", color = MeowfiaColors.TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (slot.profileBitmap != null) {
                    ProfileThumbnail(bitmap = slot.profileBitmap, size = 44)
                } else {
                    val bitmap = remember(index) { generateColorProfile(index) }
                    ProfileThumbnail(bitmap = bitmap, size = 44)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        slot.isBot -> slot.name ?: "Bot"
                        slot.name != null -> slot.name
                        else -> "P${index + 1}"
                    },
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 9.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }

        // Add button
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp).height(66.dp).clickable { onAdd() }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(2.dp, MeowfiaColors.Primary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = MeowfiaColors.Primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text("Add", color = MeowfiaColors.TextSecondary, fontSize = 9.sp)
            }
        }
    }
}

// --- Player Customization Overlay ---

@Composable
private fun PlayerCustomizationOverlay(
    slot: PreConfiguredPlayer,
    playerIndex: Int,
    onUpdate: (PreConfiguredPlayer) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(playerIndex) { mutableStateOf(slot.name ?: "") }
    var profile by remember(playerIndex) { mutableStateOf(slot.profileBitmap) }
    var isBot by remember(playerIndex) { mutableStateOf(slot.isBot) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeowfiaColors.Background.copy(alpha = 0.85f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(MeowfiaColors.Surface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Customize Player ${playerIndex + 1}",
                color = MeowfiaColors.Primary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Name", color = MeowfiaColors.TextSecondary) },
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

            if (!isBot) {
                ProfilePicturePicker(
                    playerIndex = playerIndex,
                    onProfileChanged = { bmp -> profile = bmp },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            MeowfiaSecondaryButton(
                text = if (isBot) "Make Human" else "Make Bot",
                onClick = { isBot = !isBot }
            )
            Spacer(modifier = Modifier.height(12.dp))

            MeowfiaPrimaryButton(
                text = "Done",
                onClick = {
                    val finalName = name.trim().ifEmpty { null }
                    onUpdate(PreConfiguredPlayer(name = finalName, profileBitmap = profile, isBot = isBot))
                    onDismiss()
                }
            )
        }
    }
}

// --- Pool Bubble Row (for roles) ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PoolBubbleRow(
    roles: List<RoleId>,
    onRemove: (RoleId) -> Unit,
    onLongPress: (RoleId) -> Unit
) {
    val grouped = remember(roles.toList()) {
        val counts = mutableMapOf<RoleId, Int>()
        for (r in roles) counts[r] = (counts[r] ?: 0) + 1
        val seen = mutableSetOf<RoleId>()
        roles.mapNotNull { r -> if (seen.add(r)) r to counts[r]!! else null }
    }

    Column {
        Text("Pool: ${roles.size} cards", color = MeowfiaColors.TextSecondary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(grouped) { _, (role, count) ->
                Box(
                    modifier = Modifier.combinedClickable(
                        onClick = { if (!role.isBuffer) onRemove(role) },
                        onLongClick = { onLongPress(role) }
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(56.dp).height(70.dp)
                    ) {
                        Box {
                            RoleIcon(roleId = role, size = 44)
                            if (count > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(20.dp)
                                        .background(MeowfiaColors.Secondary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$count", color = MeowfiaColors.TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (role.isBuffer) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 2.dp, y = 2.dp)
                                        .size(14.dp)
                                        .background(MeowfiaColors.Primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("B", color = MeowfiaColors.TextOnPrimary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
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
