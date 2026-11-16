package com.muswall.lite.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muswall.lite.domain.model.ScreenConfig

@Composable
fun EffectControlsCard(
    currentConfig: ScreenConfig,
    screenLabel: String,
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Wallpaper Effect",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Choose how music artwork is displayed on $screenLabel",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentConfig.effectType == ScreenConfig.EFFECT_BLUR,
                    onClick = { onConfigChange(currentConfig.copy(effectType = ScreenConfig.EFFECT_BLUR)) },
                    label = { Text("Blur", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = currentConfig.effectType == ScreenConfig.EFFECT_STILL,
                    onClick = { onConfigChange(currentConfig.copy(effectType = ScreenConfig.EFFECT_STILL)) },
                    label = { Text("Still", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = currentConfig.effectType == ScreenConfig.EFFECT_BLUR_STILL,
                    onClick = { onConfigChange(currentConfig.copy(effectType = ScreenConfig.EFFECT_BLUR_STILL)) },
                    label = { Text("Blur + Still", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.weight(1.2f)
                )
            }

            if (currentConfig.effectType == ScreenConfig.EFFECT_BLUR) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Aspect Ratio Presentation",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentConfig.aspectFitMode == ScreenConfig.ASPECT_FIT_WIDTH,
                        onClick = { onConfigChange(currentConfig.copy(aspectFitMode = ScreenConfig.ASPECT_FIT_WIDTH)) },
                        label = { Text("🖼️ Fit Width (Ambient)", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = currentConfig.aspectFitMode == ScreenConfig.ASPECT_FILL,
                        onClick = { onConfigChange(currentConfig.copy(aspectFitMode = ScreenConfig.ASPECT_FILL)) },
                        label = { Text("📐 Full Bleed (Fill)", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
