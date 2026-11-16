package com.muswall.lite.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muswall.lite.WallpaperRenderer
import com.muswall.lite.domain.model.ScreenConfig

@Composable
fun FramingCard(
    currentConfig: ScreenConfig,
    screenLabel: String,
    activeArt: Bitmap?,
    safeZoneOffset: Int,
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
                text = "Wallpaper Framing & Alignment",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = "Adjust framing and alignment of cover wallpaper on $screenLabel",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            // Horizontal Framing
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Horizontal Framing", fontSize = 13.sp)
                    val xAlignLabel = when {
                        currentConfig.autoHorizontalFraming -> {
                            val detected = activeArt?.let { WallpaperRenderer.getFocalXPercent(it) } ?: 0
                            val dir = when {
                                detected <= -15 -> "Left Focus ($detected%)"
                                detected >= 15 -> "Right Focus (+$detected%)"
                                else -> "Center ($detected%)"
                            }
                            "🧠 Context-Aware ($dir)"
                        }
                        currentConfig.wallpaperOffsetXPercent == 0 -> "Center (0%)"
                        currentConfig.wallpaperOffsetXPercent <= -70 -> "Left Focus (${currentConfig.wallpaperOffsetXPercent}%)"
                        currentConfig.wallpaperOffsetXPercent >= 70 -> "Right Focus (+${currentConfig.wallpaperOffsetXPercent}%)"
                        currentConfig.wallpaperOffsetXPercent < 0 -> "Left ${-currentConfig.wallpaperOffsetXPercent}%"
                        else -> "Right +${currentConfig.wallpaperOffsetXPercent}%"
                    }
                    Text(xAlignLabel, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                val sliderValue = if (currentConfig.autoHorizontalFraming) {
                    (activeArt?.let { WallpaperRenderer.getFocalXPercent(it) } ?: currentConfig.wallpaperOffsetXPercent).toFloat()
                } else {
                    currentConfig.wallpaperOffsetXPercent.toFloat()
                }

                Slider(
                    value = sliderValue.coerceIn(-100f, 100f),
                    onValueChange = {
                        onConfigChange(currentConfig.copy(autoHorizontalFraming = false, wallpaperOffsetXPercent = it.toInt()))
                    },
                    valueRange = -100f..100f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isContextAware = currentConfig.autoHorizontalFraming
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isContextAware) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isContextAware) MaterialTheme.colorScheme.primary else Color.Transparent),
                        modifier = Modifier
                            .weight(1.3f)
                            .clickable {
                                onConfigChange(currentConfig.copy(autoHorizontalFraming = true))
                            }
                    ) {
                        Text(
                            text = "🧠 Context-Aware",
                            fontSize = 11.sp,
                            fontWeight = if (isContextAware) FontWeight.Bold else FontWeight.Normal,
                            color = if (isContextAware) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    listOf(
                        "Left Focus" to -80,
                        "Center" to 0,
                        "Right Focus" to 80
                    ).forEach { (label, value) ->
                        val isSelected = !currentConfig.autoHorizontalFraming && currentConfig.wallpaperOffsetXPercent == value
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onConfigChange(currentConfig.copy(autoHorizontalFraming = false, wallpaperOffsetXPercent = value))
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Vertical Framing
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vertical Framing", fontSize = 13.sp)
                    val alignLabel = when {
                        currentConfig.wallpaperOffsetYPercent == safeZoneOffset -> "Safe Zone (${if (safeZoneOffset < 0) "" else "+"}${safeZoneOffset}%)"
                        currentConfig.wallpaperOffsetYPercent == 0 -> "Center (0%)"
                        currentConfig.wallpaperOffsetYPercent <= -70 -> "Top Focus (${currentConfig.wallpaperOffsetYPercent}%)"
                        currentConfig.wallpaperOffsetYPercent >= 70 -> "Bottom Focus (+${currentConfig.wallpaperOffsetYPercent}%)"
                        currentConfig.wallpaperOffsetYPercent < 0 -> "Up ${-currentConfig.wallpaperOffsetYPercent}%"
                        else -> "Down +${currentConfig.wallpaperOffsetYPercent}%"
                    }
                    Text(alignLabel, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Slider(
                    value = currentConfig.wallpaperOffsetYPercent.toFloat(),
                    onValueChange = {
                        onConfigChange(currentConfig.copy(wallpaperOffsetYPercent = it.toInt()))
                    },
                    valueRange = -100f..100f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "✨ Safe Zone" to safeZoneOffset,
                        "Top Focus" to -80,
                        "Center" to 0,
                        "Bottom Focus" to 80
                    ).forEach { (label, value) ->
                        val isSelected = currentConfig.wallpaperOffsetYPercent == value
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onConfigChange(currentConfig.copy(wallpaperOffsetYPercent = value))
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
