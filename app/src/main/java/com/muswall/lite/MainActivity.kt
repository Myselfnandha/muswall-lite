package com.muswall.lite

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MusWallLiteTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MusWallLiteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF64B5F6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current

    var blurValue by remember { mutableFloatStateOf(WallpaperState.getBlur(context).toFloat()) }
    var dimValue by remember { mutableFloatStateOf(WallpaperState.getDim(context).toFloat()) }
    var selectedMode by remember { mutableIntStateOf(WallpaperState.getMode(context)) }

    val isPlaying by WallpaperState.isPlaying.collectAsState()
    val trackTitle by WallpaperState.trackTitle.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MusWall Lite", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isPlaying) "Now Playing" else "Player Status",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = trackTitle ?: "No active playback detected",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Display Style", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = selectedMode == WallpaperState.MODE_BLURRED_STRETCH,
                            onClick = {
                                selectedMode = WallpaperState.MODE_BLURRED_STRETCH
                                WallpaperState.setMode(context, selectedMode)
                            },
                            label = { Text("Stretched Blur") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedMode == WallpaperState.MODE_EDGE_TO_EDGE,
                            onClick = {
                                selectedMode = WallpaperState.MODE_EDGE_TO_EDGE
                                WallpaperState.setMode(context, selectedMode)
                            },
                            label = { Text("Full Bleed") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (selectedMode == WallpaperState.MODE_BLURRED_STRETCH) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gaussian Blur", fontWeight = FontWeight.SemiBold)
                            Text("${blurValue.toInt()}%", color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = blurValue,
                            onValueChange = {
                                blurValue = it
                                WallpaperState.setBlur(context, it.toInt())
                            },
                            valueRange = 0f..100f
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Vignette / Dimming", fontWeight = FontWeight.SemiBold)
                        Text("${dimValue.toInt()}%", color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = dimValue,
                        onValueChange = {
                            dimValue = it
                            WallpaperState.setDim(context, it.toInt())
                        },
                        valueRange = 0f..80f
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, AlbumArtWallpaperService::class.java)
                        )
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Apply as Live Wallpaper", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Notification Access Settings")
            }
        }
    }
}
