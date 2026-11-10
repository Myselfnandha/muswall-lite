package com.muswall.lite

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.text.style.TextAlign
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
            background = Color(0xFF0F0F14),
            surface = Color(0xFF16161E),
            surfaceVariant = Color(0xFF20202C),
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
    val homeRestoreArt by WallpaperState.homeRestoreArt.collectAsState()
    val lockRestoreArt by WallpaperState.lockRestoreArt.collectAsState()
    val isPlaying by WallpaperState.isPlaying.collectAsState()
    val trackTitle by WallpaperState.trackTitle.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var previewShowingRestore by remember { mutableStateOf(false) }

    val homePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = loadSoftwareBitmapFromUri(context, uri)
            if (bmp != null) {
                if (separateScreens) {
                    WallpaperState.setHomeRestoreBitmap(context, bmp)
                } else {
                    WallpaperState.setUnifiedRestoreBitmap(context, bmp)
                }
            }
        }
    }

    val lockPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = loadSoftwareBitmapFromUri(context, uri)
            if (bmp != null) {
                WallpaperState.setLockRestoreBitmap(context, bmp)
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

    val homePreviewBitmap = remember(displayArt, homeRestoreArt, previewShowingRestore, homeConfig) {
        renderScreenPreview(
            art = displayArt,
            restore = homeRestoreArt,
            isRestore = previewShowingRestore,
            config = homeConfig
        )
    }

    val lockPreviewBitmap = remember(displayArt, lockRestoreArt, homeRestoreArt, previewShowingRestore, lockConfig, separateScreens) {
        val effectiveLockRestore = if (separateScreens) (lockRestoreArt ?: homeRestoreArt) else homeRestoreArt
        val effectiveLockConfig = if (separateScreens) lockConfig else homeConfig
        renderScreenPreview(
            art = displayArt,
            restore = effectiveLockRestore,
            isRestore = previewShowingRestore,
            config = effectiveLockConfig
        )
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
                            text = if (separateScreens) "Separate Screens" else "Linked",
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
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Screen Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (previewShowingRestore) "Restored Default Wallpaper" else (trackTitle ?: "Music Album Artwork"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        FilledTonalButton(
                            onClick = { previewShowingRestore = !previewShowingRestore },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (previewShowingRestore) "🖼️ Default" else "🎵 Music Blur",
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { activeTab = 0 }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(115.dp)
                                    .height(245.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        width = if (activeTab == 0) 2.5.dp else 1.dp,
                                        color = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color(0xFF333345),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = homePreviewBitmap.asImageBitmap(),
                                    contentDescription = "Home Screen Live Preview",
                                    modifier = Modifier.fillMaxSize()
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(bottom = 12.dp),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        repeat(4) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0x77FFFFFF))
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🏠 Home Screen",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { activeTab = 1 }
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(115.dp)
                                    .height(245.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(
                                        width = if (activeTab == 1) 2.5.dp else 1.dp,
                                        color = if (activeTab == 1) MaterialTheme.colorScheme.primary else Color(0xFF333345),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = lockPreviewBitmap.asImageBitmap(),
                                    contentDescription = "Lock Screen Live Preview",
                                    modifier = Modifier.fillMaxSize()
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.Top,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "12:45",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Tuesday, Nov 10",
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "🔒 Lock Screen",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.LightGray
                            )
                        }
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
                        text = { Text("🏠 Home Screen Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("🔒 Lock Screen Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
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
                        text = "Control Sliders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Blur", fontSize = 13.sp)
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
                    Text("Restore Wallpaper (Default Wallpaper)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "When music pauses or stops, the wallpaper automatically restores to your default phone wallpaper.",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 75.dp, height = 120.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0xFF444455), RoundedCornerShape(10.dp))
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                if (homeRestoreArt != null) {
                                    Image(
                                        bitmap = homeRestoreArt!!.asImageBitmap(),
                                        contentDescription = "Home Default Wallpaper",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text("Default", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Home Default", fontSize = 11.sp, color = Color.White)
                        }

                        if (separateScreens) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 75.dp, height = 120.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0xFF444455), RoundedCornerShape(10.dp))
                                        .background(Color.DarkGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val lockArt = lockRestoreArt ?: homeRestoreArt
                                    if (lockArt != null) {
                                        Image(
                                            bitmap = lockArt.asImageBitmap(),
                                            contentDescription = "Lock Default Wallpaper",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text("Default", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Lock Default", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }

                    Text("Delay before restoring default wallpaper:", fontSize = 13.sp)
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
                                WallpaperState.captureSystemDefaultWallpaper(context)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Capture System Wallpaper", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                    context.startActivity(Intent.createChooser(intent, "Select Wallpaper"))
                                } catch (_: Exception) {
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("System Settings", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                homePhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (separateScreens) "Pick Home Wallpaper" else "Pick Custom Wallpaper", fontSize = 11.sp)
                        }

                        if (separateScreens) {
                            OutlinedButton(
                                onClick = {
                                    lockPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Pick Lock Wallpaper", fontSize = 11.sp)
                            }
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

fun renderScreenPreview(
    art: Bitmap?,
    restore: Bitmap?,
    isRestore: Boolean,
    config: ScreenConfig
): Bitmap {
    val pw = 280
    val ph = 600
    val bmp = Bitmap.createBitmap(pw, ph, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    WallpaperRenderer.render(
        canvas = canvas,
        width = pw,
        height = ph,
        artBitmap = art,
        restoreBitmap = restore,
        isRestoreActive = isRestore,
        config = config,
        transitionProgress = 1f,
        transitionMode = 0
    )
    return bmp
}

fun loadSoftwareBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (_: Exception) {
        null
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
