package com.meowfia.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.data.model.RoleId
import com.meowfia.app.ui.components.MeowfiaSecondaryButton
import com.meowfia.app.ui.components.RoleCardOverlay
import com.meowfia.app.ui.components.RoleGrid
import com.meowfia.app.ui.theme.MeowfiaColors

@Composable
fun PoolViewerScreen(
    onBack: () -> Unit
) {
    var overlayRole by remember { mutableStateOf<RoleId?>(null) }

    val roles = remember {
        RoleId.entries.filter { it.implemented }
    }

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

            Spacer(modifier = Modifier.height(16.dp))
            MeowfiaSecondaryButton(text = "Back", onClick = onBack)
        }

        RoleCardOverlay(roleId = overlayRole, onDismiss = { overlayRole = null })
    }
}
