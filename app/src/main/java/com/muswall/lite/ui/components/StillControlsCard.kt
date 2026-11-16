package com.muswall.lite.ui.components

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
import com.muswall.lite.domain.model.ScreenConfig

@Composable
fun StillControlsCard(
    currentConfig: ScreenConfig,
    safeZoneOffset: Int,
    onConfigChange: (ScreenConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentConfig.effectType == ScreenConfig.EFFECT_BLUR) return

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
                text = "Music Still Artwork Controls",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vertical Position", fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val offsetText = when {
                            currentConfig.stillOffsetYPercent == safeZoneOffset -> "Safe Zone (${if (safeZoneOffset < 0) "" else "+"}${safeZoneOffset}%)"
                            currentConfig.stillOffsetYPercent == -25 -> "Upper (-25%)"
                            currentConfig.stillOffsetYPercent == 0 -> "Center (0%)"
                            currentConfig.stillOffsetYPercent < 0 -> "Up ${-currentConfig.stillOffsetYPercent}%"
                            else -> "Down +${currentConfig.stillOffsetYPercent}%"
                        }
                        Text(offsetText, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        if (currentConfig.stillOffsetYPercent != safeZoneOffset) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Auto",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.clickable {
                                    onConfigChange(currentConfig.copy(stillOffsetYPercent = safeZoneOffset))
                                }
                            )
                        }
                    }
                }
                Slider(
                    value = currentConfig.stillOffsetYPercent.toFloat(),
                    onValueChange = {
                        onConfigChange(currentConfig.copy(stillOffsetYPercent = it.toInt()))
                    },
                    valueRange = -100f..100f
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "✨ Safe Zone" to safeZoneOffset,
                        "Top" to -75,
                        "Upper" to -25,
                        "Center" to 0,
                        "Lower" to 40
                    ).forEach { (label, value) ->
                        val isSelected = currentConfig.stillOffsetYPercent == value
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .weight(if (label.startsWith("✨")) 1.4f else 1f)
                                .clickable {
                                    onConfigChange(currentConfig.copy(stillOffsetYPercent = value))
                                }
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Artwork Scale", fontSize = 13.sp)
                    Text("${currentConfig.stillScalePercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Slider(
                    value = currentConfig.stillScalePercent.toFloat(),
                    onValueChange = {
                        onConfigChange(currentConfig.copy(stillScalePercent = it.toInt()))
                    },
                    valueRange = 30f..120f
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Corner Radius", fontSize = 13.sp)
                    Text("${currentConfig.stillCornerRadiusPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Slider(
                    value = currentConfig.stillCornerRadiusPercent.toFloat(),
                    onValueChange = {
                        onConfigChange(currentConfig.copy(stillCornerRadiusPercent = it.toInt()))
                    },
                    valueRange = 0f..50f
                )
            }
        }
    }
}
