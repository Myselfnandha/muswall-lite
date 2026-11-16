package com.muswall.lite.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muswall.lite.domain.model.ScreenConfig

@Composable
fun RecoverWallpaperCard(
    separateScreens: Boolean,
    recoverTab: Int,
    onRecoverTabChange: (Int) -> Unit,
    activeRecoverConfig: ScreenConfig,
    currentRestoreArt: Bitmap,
    isCustomArt: Boolean,
    previewShowingRestore: Boolean,
    onTogglePreviewRestore: () -> Unit,
    onPickImage: () -> Unit,
    onCaptureSystem: () -> Unit,
    onOpenSystemWallpaperChooser: () -> Unit,
    onRecoverConfigChange: (ScreenConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenTitle = if (separateScreens) (if (recoverTab == 1) "Lock Screen" else "Home Screen") else "All Screens"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Recover Wallpaper (Default)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = if (separateScreens) "Separate wallpaper & effects for each screen" else "Wallpaper restored when playback stops",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
                FilledTonalButton(
                    onClick = onTogglePreviewRestore,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (previewShowingRestore) "🎵 Live Music" else "🖼️ Live Recover",
                        fontSize = 11.sp
                    )
                }
            }

            if (separateScreens) {
                TabRow(
                    selectedTabIndex = recoverTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = recoverTab == 0,
                        onClick = { onRecoverTabChange(0) },
                        text = { Text("🏠 Home Recover", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = recoverTab == 1,
                        onClick = { onRecoverTabChange(1) },
                        text = { Text("🔒 Lock Recover", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 75.dp, height = 120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        .background(Color.DarkGray)
                        .clickable { onPickImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = currentRestoreArt.asImageBitmap(),
                        contentDescription = "$screenTitle Wallpaper",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$screenTitle Wallpaper",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isCustomArt) "Custom image selected" else "Sample default wallpaper",
                        fontSize = 11.sp,
                        color = if (isCustomArt) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Button(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Choose Image", fontSize = 11.sp)
                    }
                }
            }

            Text("Delay before restoring $screenTitle wallpaper:", fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "0s" to 0,
                    "15s" to 15,
                    "30s" to 30,
                    "1m" to 60,
                    "5m" to 300,
                    "Never" to -1
                ).forEach { (label, sec) ->
                    FilterChip(
                        selected = activeRecoverConfig.restoreDelaySeconds == sec,
                        onClick = { onRecoverConfigChange(activeRecoverConfig.copy(restoreDelaySeconds = sec)) },
                        label = { Text(label, fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF20202E))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Apply effects to $screenTitle Recover Wallpaper", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(
                        text = "Apply blur and dimming to $screenTitle restored wallpaper",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                }
                Switch(
                    checked = activeRecoverConfig.applyEffectsToRestore,
                    onCheckedChange = {
                        onRecoverConfigChange(activeRecoverConfig.copy(applyEffectsToRestore = it))
                    },
                    modifier = Modifier.scale(0.85f)
                )
            }

            if (activeRecoverConfig.applyEffectsToRestore) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$screenTitle Recover Blur Intensity", fontSize = 13.sp)
                        Text("${activeRecoverConfig.restoreBlurPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                    Slider(
                        value = activeRecoverConfig.restoreBlurPercent.toFloat(),
                        onValueChange = {
                            onRecoverConfigChange(activeRecoverConfig.copy(restoreBlurPercent = it.toInt()))
                        },
                        valueRange = 0f..100f
                    )
                }

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$screenTitle Recover Dimming", fontSize = 13.sp)
                        Text("${activeRecoverConfig.restoreDimPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                    }
                    Slider(
                        value = activeRecoverConfig.restoreDimPercent.toFloat(),
                        onValueChange = {
                            onRecoverConfigChange(activeRecoverConfig.copy(restoreDimPercent = it.toInt()))
                        },
                        valueRange = 0f..80f
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPickImage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Pick & Set Dimensions", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                OutlinedButton(
                    onClick = onCaptureSystem,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Capture System", fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }

            OutlinedButton(
                onClick = onOpenSystemWallpaperChooser,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open System Wallpaper Setter Screen", fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
