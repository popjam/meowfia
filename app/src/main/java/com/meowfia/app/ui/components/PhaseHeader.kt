package com.meowfia.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meowfia.app.ui.theme.MeowfiaColors

/** Small top bar showing current round and phase, e.g. "Round 3 · Night Phase". */
@Composable
fun PhaseHeader(
    roundNumber: Int,
    phaseName: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Round $roundNumber · $phaseName",
        color = MeowfiaColors.TextSecondary,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
