package com.muswall.lite

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperState.init(applicationContext)
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
            secondary = Color(0xFF81C784),
            background = Color(0xFF101014),
            surface = Color(0xFF181820),
            surfaceVariant = Color(0xFF22222C),
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

    val homeConfig by WallpaperState.homeConfig.collectAsState()
    val lockConfig by WallpaperState.lockConfig.collectAsState()
    val separateScreens by WallpaperState.separateScreens.collectAsState()

    val currentArt by WallpaperState.currentArt.collectAsState()
    val restoreArt by WallpaperState.restoreArt.collectAsState()
    val isPlaying by WallpaperState.isPlaying.collectAsState()
    val trackTitle by WallpaperState.trackTitle.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var previewTestingRestore by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                WallpaperState.setRestoreBitmap(context, bitmap)
            } catch (_: Exception) {
            }
        }
    }

    val sampleArt = remember { generateSampleArtwork() }
    val displayArt = currentArt ?: sampleArt

    val currentConfig = if (separateScreens && activeTab == 1) lockConfig else homeConfig

    fun updateCurrentConfig(newConfig: ScreenConfig) {
        if (separateScreens && activeTab == 1) {
            WallpaperState.updateLockConfig(context, newConfig)
        } else {
            WallpaperState.updateHomeConfig(context, newConfig)
        }
    }

    val previewBitmap = remember(displayArt, restoreArt, previewTestingRestore, currentConfig) {
        val pw = 360
        val ph = 760
        val bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        WallpaperRenderer.render(
            canvas = canvas,
            width = pw,
            height = ph,
            artBitmap = displayArt,
            restoreBitmap = restoreArt,
            isRestoreActive = previewTestingRestore,
            config = currentConfig,
            transitionProgress = 1f,
            transitionMode = 0
        )
        bmp
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MusWall Lite", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            text = if (separateScreens) "Separate Profiles" else "Linked Profiles",
                            fontSize = 12.sp,
                            color = if (separateScreens) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = separateScreens,
                            onCheckedChange = { WallpaperState.setSeparateScreens(context, it) },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Live Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = trackTitle ?: (if (currentArt != null) "Active Music Stream" else "Sample Cover Preview"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        FilledTonalButton(
                            onClick = { previewTestingRestore = !previewTestingRestore },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (previewTestingRestore) "Restore Active" else "Playing Art",
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(285.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(2.dp, Color(0xFF333344), RoundedCornerShape(22.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Wallpaper Live Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            if (separateScreens) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("🏠 Home Screen", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("🔒 Lock Screen", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Wallpaper Effect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentConfig.effectType == ScreenConfig.EFFECT_COVER_CARD,
                            onClick = {
                                updateCurrentConfig(currentConfig.copy(effectType = ScreenConfig.EFFECT_COVER_CARD))
                            },
                            label = { Text("Cover Card", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentConfig.effectType == ScreenConfig.EFFECT_FULL_BLEED,
                            onClick = {
                                updateCurrentConfig(currentConfig.copy(effectType = ScreenConfig.EFFECT_FULL_BLEED))
                            },
                            label = { Text("Full Bleed", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentConfig.effectType == ScreenConfig.EFFECT_STRETCHED_BLUR,
                            onClick = {
                                updateCurrentConfig(currentConfig.copy(effectType = ScreenConfig.EFFECT_STRETCHED_BLUR))
                            },
                            label = { Text("Stretch Blur", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Effect Sliders & Layout",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Gaussian Blur", fontSize = 13.sp)
                            Text("${currentConfig.blurPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                        Slider(
                            value = currentConfig.blurPercent.toFloat(),
                            onValueChange = {
                                updateCurrentConfig(currentConfig.copy(blurPercent = it.toInt()))
                            },
                            valueRange = 0f..100f
                        )
                    }

                    if (currentConfig.effectType == ScreenConfig.EFFECT_COVER_CARD) {
                        Column {
                            Text("Cover Card Position", fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Top" to ScreenConfig.POSITION_TOP,
                                    "Center" to ScreenConfig.POSITION_CENTER,
                                    "Bottom" to ScreenConfig.POSITION_BOTTOM,
                                    "Custom" to ScreenConfig.POSITION_CUSTOM
                                ).forEach { (label, pos) ->
                                    FilterChip(
                                        selected = currentConfig.coverPosition == pos,
                                        onClick = { updateCurrentConfig(currentConfig.copy(coverPosition = pos)) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cover Height / Size", fontSize = 13.sp)
                                Text("${currentConfig.coverSizePercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                            Slider(
                                value = currentConfig.coverSizePercent.toFloat(),
                                onValueChange = {
                                    updateCurrentConfig(currentConfig.copy(coverSizePercent = it.toInt()))
                                },
                                valueRange = 30f..95f
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cover Vertical Offset", fontSize = 13.sp)
                                Text("${currentConfig.coverYOffsetPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                            Slider(
                                value = currentConfig.coverYOffsetPercent.toFloat(),
                                onValueChange = {
                                    updateCurrentConfig(currentConfig.copy(coverYOffsetPercent = it.toInt()))
                                },
                                valueRange = -50f..50f
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Card Corner Radius", fontSize = 13.sp)
                                Text("${currentConfig.coverCornerRadiusDp}dp", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                            Slider(
                                value = currentConfig.coverCornerRadiusDp.toFloat(),
                                onValueChange = {
                                    updateCurrentConfig(currentConfig.copy(coverCornerRadiusDp = it.toInt()))
                                },
                                valueRange = 0f..45f
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dimming / Vignette", fontSize = 13.sp)
                            Text("${currentConfig.dimPercent}%", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        }
                        Slider(
                            value = currentConfig.dimPercent.toFloat(),
                            onValueChange = {
                                updateCurrentConfig(currentConfig.copy(dimPercent = it.toInt()))
                            },
                            valueRange = 0f..80f
                        )
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Restore Wallpaper & Timing", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Text("Delay before restoring after playback pauses:", fontSize = 13.sp)
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
                                selected = currentConfig.restoreDelaySeconds == sec,
                                onClick = { updateCurrentConfig(currentConfig.copy(restoreDelaySeconds = sec)) },
                                label = { Text(label, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pick Gallery Wallpaper", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val wm = WallpaperManager.getInstance(context)
                                    val drawable = wm.drawable
                                    if (drawable != null) {
                                        val bmp = Bitmap.createBitmap(
                                            drawable.intrinsicWidth.coerceAtLeast(1080),
                                            drawable.intrinsicHeight.coerceAtLeast(1920),
                                            Bitmap.Config.ARGB_8888
                                        )
                                        val canvas = Canvas(bmp)
                                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                                        drawable.draw(canvas)
                                        WallpaperState.setRestoreBitmap(context, bmp)
                                    }
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Capture System Wallpaper", fontSize = 11.sp)
                        }
                    }

                    if (restoreArt != null) {
                        TextButton(
                            onClick = { WallpaperState.setRestoreBitmap(context, null) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Reset to Default Ambient Dark", fontSize = 12.sp, color = Color(0xFFFF8A80))
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Apply & Restore Transitions", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    Column {
                        Text("Wallpaper Apply Effect (When music starts):", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Crossfade" to ScreenConfig.TRANSITION_FADE,
                                "Zoom & Fade" to ScreenConfig.TRANSITION_ZOOM,
                                "Instant" to ScreenConfig.TRANSITION_NONE
                            ).forEach { (label, tr) ->
                                FilterChip(
                                    selected = currentConfig.applyEffect == tr,
                                    onClick = { updateCurrentConfig(currentConfig.copy(applyEffect = tr)) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Column {
                        Text("Restore Wallpaper Effect (When music stops):", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Crossfade" to ScreenConfig.TRANSITION_FADE,
                                "Fade Out" to ScreenConfig.TRANSITION_ZOOM,
                                "Instant" to ScreenConfig.TRANSITION_NONE
                            ).forEach { (label, tr) ->
                                FilterChip(
                                    selected = currentConfig.restoreEffect == tr,
                                    onClick = { updateCurrentConfig(currentConfig.copy(restoreEffect = tr)) },
                                    label = { Text(label, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

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
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Set as Live Wallpaper", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Notification Listener Access", fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

fun generateSampleArtwork(): Bitmap {
    val size = 512
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val gradient = LinearGradient(
        0f, 0f, size.toFloat(), size.toFloat(),
        intArrayOf(AndroidColor.parseColor("#7B1FA2"), AndroidColor.parseColor("#E91E63"), AndroidColor.parseColor("#FF9800")),
        null,
        Shader.TileMode.CLAMP
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = gradient
    }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

    val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(120, 0, 0, 0)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.32f, circlePaint)

    val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size * 0.08f, innerPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText("MUSIC ART", size / 2f, size * 0.88f, textPaint)

    return bmp
}
