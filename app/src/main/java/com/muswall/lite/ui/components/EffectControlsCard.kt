package com.muswall.lite.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muswall.lite.domain.model.ScreenConfig

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EffectControlsCard(
    currentConfig: ScreenConfig,
    screenLabel: String,
    isLockScreen: Boolean,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Wallpaper Effect & Presets",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Choose curated styles or customize effects for $screenLabel",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            // Curated Quick Presets
            Text(
                text = "✨ Quick Styles",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Balanced" to ScreenConfig.PRESET_BALANCED,
                    "AMOLED Dark" to ScreenConfig.PRESET_AMOLED,
                    "Immersive Blur" to ScreenConfig.PRESET_IMMERSIVE,
                    "Minimal Card" to ScreenConfig.PRESET_MINIMAL,
                    "Poster Flow" to ScreenConfig.PRESET_POSTER
                ).forEach { (label, presetId) ->
                    AssistChip(
                        onClick = { onConfigChange(ScreenConfig.createPreset(presetId, isLockScreen)) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF282836), thickness = 0.8.dp)

            Text(
                text = "Effect Mode",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = currentConfig.effectType == ScreenConfig.EFFECT_BLUR,
                    onClick = { onConfigChange(currentConfig.copy(effectType = ScreenConfig.EFFECT_BLUR, amoledMode = false)) },
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
                    onClick = { onConfigChange(currentConfig.copy(effectType = ScreenConfig.EFFECT_BLUR_STILL, amoledMode = false)) },
                    label = { Text("Blur + Still", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.weight(1.2f)
                )
            }

            if (currentConfig.effectType == ScreenConfig.EFFECT_BLUR) {
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

            // AMOLED Mode Toggle
            if (currentConfig.effectType != ScreenConfig.EFFECT_BLUR) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF20202E))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("AMOLED Pure Black Background", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Turns off background pixels for OLED battery savings", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Switch(
                        checked = currentConfig.amoledMode,
                        onCheckedChange = { onConfigChange(currentConfig.copy(amoledMode = it)) },
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }

            // Scrim Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF20202E))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Icon Legibility Scrim", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Top/bottom soft gradient protecting clock & dock readability", fontSize = 11.sp, color = Color.LightGray)
                }
                Switch(
                    checked = currentConfig.scrimEnabled,
                    onCheckedChange = { onConfigChange(currentConfig.copy(scrimEnabled = it)) },
                    modifier = Modifier.scale(0.85f)
                )
            }

            // Track Info Overlay Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF20202E))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Display Track Title Pill", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Draws a subtle semi-transparent song title badge on wallpaper", fontSize = 11.sp, color = Color.LightGray)
                }
                Switch(
                    checked = currentConfig.showTrackInfo,
                    onCheckedChange = { onConfigChange(currentConfig.copy(showTrackInfo = it)) },
                    modifier = Modifier.scale(0.85f)
                )
            }
        }
    }
}
