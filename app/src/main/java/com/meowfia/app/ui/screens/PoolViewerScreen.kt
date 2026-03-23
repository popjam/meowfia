package com.meowfia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.CardType
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.RoleInfoCard
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun PoolViewerScreen(
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Farm Animals", "Meowfia Animals", "Flowers")
    val types = listOf(CardType.FARM_ANIMAL, CardType.MEOWFIA_ANIMAL, CardType.FLOWER)

    val roles = RoleId.entries
        .filter { it.implemented && it.cardType == types[selectedTab] }

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

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MeowfiaColors.Surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(12.dp),
                        color = MeowfiaColors.TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(roles) { role ->
                RoleInfoCard(roleId = role)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        MeowfiaSecondaryButton(text = "Back", onClick = onBack)
    }
}
