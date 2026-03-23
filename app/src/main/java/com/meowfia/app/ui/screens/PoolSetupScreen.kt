package com.meowfia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.components.MeowfiaPrimaryButton
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun PoolSetupScreen(
    onStartGame: (selectedRoles: List<RoleId>, playerCount: Int, botCount: Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // 0 = QR, 1 = Manual
    var playerCount by remember { mutableIntStateOf(6) }
    var botCount by remember { mutableIntStateOf(0) }
    val selectedRoles = remember {
        mutableStateListOf(RoleId.PIGEON, RoleId.HOUSE_CAT)
    }

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
        Spacer(modifier = Modifier.height(16.dp))

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

        Spacer(modifier = Modifier.height(16.dp))

        // Tab selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MeowfiaColors.Surface
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Scan QR", modifier = Modifier.padding(12.dp), color = MeowfiaColors.TextPrimary)
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Select Manually", modifier = Modifier.padding(12.dp), color = MeowfiaColors.TextPrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // QR tab placeholder
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("QR Scanner", color = MeowfiaColors.TextSecondary, fontSize = 18.sp)
                    Text("Camera preview will appear here", color = MeowfiaColors.TextSecondary, fontSize = 14.sp)
                }
            }
            1 -> {
                // Manual selection
                val implementedRoles = RoleId.entries.filter { it.implemented && !it.isBuffer }
                val grouped = implementedRoles.groupBy { it.cardType }

                Text(
                    text = "Pool: ${selectedRoles.size} cards (2 base + ${selectedRoles.size - 2} selected)",
                    color = MeowfiaColors.TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    for ((type, roles) in grouped) {
                        item {
                            val label = when (type) {
                                CardType.FARM_ANIMAL -> "Farm Animals"
                                CardType.MEOWFIA_ANIMAL -> "Meowfia Animals"
                                CardType.FLOWER -> "Flowers"
                            }
                            Text(
                                text = label,
                                color = when (type) {
                                    CardType.FARM_ANIMAL -> MeowfiaColors.Farm
                                    CardType.MEOWFIA_ANIMAL -> MeowfiaColors.Meowfia
                                    CardType.FLOWER -> MeowfiaColors.Confused
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(roles) { role ->
                            val isSelected = role in selectedRoles
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedRoles.add(role)
                                        else selectedRoles.remove(role)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MeowfiaColors.Primary,
                                        uncheckedColor = MeowfiaColors.TextSecondary
                                    )
                                )
                                Column {
                                    Text(role.displayName, color = MeowfiaColors.TextPrimary, fontSize = 16.sp)
                                    Text(role.description, color = MeowfiaColors.TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        MeowfiaPrimaryButton(
            text = "Start Game",
            onClick = { onStartGame(selectedRoles.toList(), playerCount, botCount) },
            enabled = selectedRoles.size > 2
        )
        Spacer(modifier = Modifier.height(8.dp))
        MeowfiaSecondaryButton(
            text = "Use Default Roles",
            onClick = {
                val defaults = listOf(
                    RoleId.PIGEON, RoleId.HOUSE_CAT,
                    RoleId.HAWK, RoleId.OWL, RoleId.EAGLE
                )
                onStartGame(defaults, playerCount, botCount)
            }
        )
    }
}
