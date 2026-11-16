package com.muswall.lite.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muswall.lite.domain.model.ScreenConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransitionsCard(
    currentConfig: ScreenConfig,
    onConfigChange: (ScreenConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val transitionsList = listOf(
        "Crossfade" to ScreenConfig.TRANSITION_FADE,
        "Zoom & Fade" to ScreenConfig.TRANSITION_ZOOM,
        "Slide Up" to ScreenConfig.TRANSITION_SLIDE_UP,
        "Slide Down" to ScreenConfig.TRANSITION_SLIDE_DOWN,
        "Radial Expand" to ScreenConfig.TRANSITION_RADIAL,
        "Blur Dissolve" to ScreenConfig.TRANSITION_BLUR_DISSOLVE,
        "Instant" to ScreenConfig.TRANSITION_NONE
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Apply & Restore Transition Effects", fontWeight = FontWeight.Bold, fontSize = 15.sp)

            Column {
                Text("Wallpaper Apply Transition (When music starts):", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    transitionsList.forEach { (label, tr) ->
                        FilterChip(
                            selected = currentConfig.applyEffect == tr,
                            onClick = { onConfigChange(currentConfig.copy(applyEffect = tr)) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            Column {
                Text("Restore Wallpaper Transition (When music stops):", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    transitionsList.forEach { (label, tr) ->
                        FilterChip(
                            selected = currentConfig.restoreEffect == tr,
                            onClick = { onConfigChange(currentConfig.copy(restoreEffect = tr)) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}
