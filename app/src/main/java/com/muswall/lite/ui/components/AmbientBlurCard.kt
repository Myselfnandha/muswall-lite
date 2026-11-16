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

@Composable
fun AmbientBlurCard(
    currentConfig: ScreenConfig,
    onConfigChange: (ScreenConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (currentConfig.effectType == ScreenConfig.EFFECT_STILL) "Background Controls" else "Ambient Blur Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            if (currentConfig.effectType != ScreenConfig.EFFECT_STILL) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Blur Intensity", fontSize = 13.sp)
                        Text("${currentConfig.blurPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                    Slider(
                        value = currentConfig.blurPercent.toFloat(),
                        onValueChange = {
                            onConfigChange(currentConfig.copy(blurPercent = it.toInt()))
                        },
                        valueRange = 0f..100f
                    )
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dimming", fontSize = 13.sp)
                    Text("${currentConfig.dimPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Slider(
                    value = currentConfig.dimPercent.toFloat(),
                    onValueChange = {
                        onConfigChange(currentConfig.copy(dimPercent = it.toInt()))
                    },
                    valueRange = 0f..80f
                )
            }
        }
    }
}
