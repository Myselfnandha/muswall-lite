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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.core.content.FileProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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

    override fun onResume() {
        super.onResume()
        if (WallpaperState.homeRestoreArt.value == null) {
            WallpaperState.captureSystemDefaultWallpaper(applicationContext)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val homeConfig by WallpaperState.homeConfig.collectAsState()
    val lockConfig by WallpaperState.lockConfig.collectAsState()
    val separateScreens by WallpaperState.separateScreens.collectAsState()

    var localHomeConfig by remember(homeConfig) { mutableStateOf(homeConfig) }
    var localLockConfig by remember(lockConfig) { mutableStateOf(lockConfig) }
    var localSeparateScreens by remember(separateScreens) { mutableStateOf(separateScreens) }

    val hasUnsavedChanges = (localHomeConfig != homeConfig || localLockConfig != lockConfig || localSeparateScreens != separateScreens)

    fun applyAndSaveChanges() {
        WallpaperState.updateHomeConfig(context, localHomeConfig)
        WallpaperState.updateLockConfig(context, localLockConfig)
        WallpaperState.setSeparateScreens(context, localSeparateScreens)
        Toast.makeText(context, "Changes saved and applied!", Toast.LENGTH_SHORT).show()
    }

    val currentArt by WallpaperState.currentArt.collectAsState()
    val homeRestoreArt by WallpaperState.homeRestoreArt.collectAsState()
    val lockRestoreArt by WallpaperState.lockRestoreArt.collectAsState()
    val preparedHomeBlur by WallpaperState.preparedHomeBlur.collectAsState()
    val preparedLockBlur by WallpaperState.preparedLockBlur.collectAsState()
    val isPlaying by WallpaperState.isPlaying.collectAsState()
    val trackTitle by WallpaperState.trackTitle.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var previewShowingRestore by remember { mutableStateOf(false) }
    var previewAnimProgress by remember { mutableFloatStateOf(1f) }

    if (!localSeparateScreens && activeTab == 1) {
        activeTab = 0
    }

    fun playPreviewTransition(targetRestoreState: Boolean) {
        coroutineScope.launch {
            previewShowingRestore = targetRestoreState
            val frames = 18
            val delayMs = 16L
            for (i in 1..frames) {
                val t = i / frames.toFloat()
                previewAnimProgress = 1f - (1f - t) * (1f - t)
                delay(delayMs)
            }
            previewAnimProgress = 1f
        }
    }

    val cropWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        WallpaperState.captureSystemDefaultWallpaper(context)
    }

    fun launchSystemDimensionSetter(sourceUri: Uri) {
        val bmp = loadSoftwareBitmapFromUri(context, sourceUri)
        if (bmp != null) {
            if (localSeparateScreens && activeTab == 1) {
                WallpaperState.setLockRestoreBitmap(context, bmp)
            } else {
                WallpaperState.setUnifiedRestoreBitmap(context, bmp)
            }
        }

        try {
            val cacheFile = File(context.cacheDir, "wallpaper_crop_input.png")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            val cropIntent = WallpaperManager.getInstance(context).getCropAndSetWallpaperIntent(contentUri)
            cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            cropWallpaperLauncher.launch(cropIntent)
        } catch (_: Exception) {
        }
    }

    val homePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            launchSystemDimensionSetter(uri)
        }
    }

    val lockPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            launchSystemDimensionSetter(uri)
        }
    }

    val sampleArt = remember { generateSampleArtwork() }
    val displayArt = currentArt ?: sampleArt

    val currentConfig = if (localSeparateScreens && activeTab == 1) localLockConfig else localHomeConfig

    fun updateCurrentConfig(newConfig: ScreenConfig) {
        if (localSeparateScreens && activeTab == 1) {
            localLockConfig = newConfig
        } else {
            localHomeConfig = newConfig
        }
    }

    val homePreviewBitmap = remember(
        displayArt,
        homeRestoreArt,
        previewShowingRestore,
        localHomeConfig,
        previewAnimProgress,
        preparedHomeBlur
    ) {
        val mode = if (previewShowingRestore) localHomeConfig.restoreEffect else localHomeConfig.applyEffect
        val previous = if (previewShowingRestore) displayArt else homeRestoreArt
        val blurBitmap = if (previewShowingRestore) null else (if (localHomeConfig.blurPercent == homeConfig.blurPercent) preparedHomeBlur else null) ?: displayArt
        renderScreenPreview(
            art = displayArt,
            restore = homeRestoreArt,
            isRestore = previewShowingRestore,
            config = localHomeConfig,
            progress = previewAnimProgress,
            mode = mode,
            previous = previous,
            preparedBlur = blurBitmap
        )
    }

    val lockPreviewBitmap = remember(
        displayArt,
        lockRestoreArt,
        homeRestoreArt,
        previewShowingRestore,
        localLockConfig,
        localSeparateScreens,
        previewAnimProgress,
        preparedLockBlur
    ) {
        val effectiveLockRestore = if (localSeparateScreens) (lockRestoreArt ?: homeRestoreArt) else homeRestoreArt
        val effectiveLockConfig = if (localSeparateScreens) localLockConfig else localHomeConfig
        val mode = if (previewShowingRestore) effectiveLockConfig.restoreEffect else effectiveLockConfig.applyEffect
        val previous = if (previewShowingRestore) displayArt else effectiveLockRestore
        val blurBitmap = if (previewShowingRestore) null else (if (effectiveLockConfig.blurPercent == lockConfig.blurPercent) preparedLockBlur else null) ?: displayArt
        renderScreenPreview(
            art = displayArt,
            restore = effectiveLockRestore,
            isRestore = previewShowingRestore,
            config = effectiveLockConfig,
            progress = previewAnimProgress,
            mode = mode,
            previous = previous,
            preparedBlur = blurBitmap
        )
    }

    val transitionsList = remember {
        listOf(
            "Crossfade" to ScreenConfig.TRANSITION_FADE,
            "Zoom & Fade" to ScreenConfig.TRANSITION_ZOOM,
            "Slide Up" to ScreenConfig.TRANSITION_SLIDE_UP,
            "Slide Down" to ScreenConfig.TRANSITION_SLIDE_DOWN,
            "Radial Expand" to ScreenConfig.TRANSITION_RADIAL,
            "Blur Dissolve" to ScreenConfig.TRANSITION_BLUR_DISSOLVE,
            "Instant" to ScreenConfig.TRANSITION_NONE
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
                        if (hasUnsavedChanges) {
                            Button(
                                onClick = { applyAndSaveChanges() },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = if (localSeparateScreens) "Separate" else "Linked",
                            fontSize = 12.sp,
                            color = if (localSeparateScreens) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = localSeparateScreens,
                            onCheckedChange = { localSeparateScreens = it },
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
                            onClick = { playPreviewTransition(!previewShowingRestore) },
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

            if (localSeparateScreens) {
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
                        text = "Selecting an image opens the system wallpaper setter so you can position, zoom, and set dimensions exactly as you prefer.",
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

                        if (localSeparateScreens) {
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
                        Button(
                            onClick = {
                                if (localSeparateScreens && activeTab == 1) {
                                    lockPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    homePhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Pick & Set Dimensions", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }

                        OutlinedButton(
                            onClick = {
                                WallpaperState.captureSystemDefaultWallpaper(context)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Capture System", fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                                context.startActivity(Intent.createChooser(intent, "System Wallpaper Setter"))
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Open System Wallpaper Setter Screen", fontSize = 12.sp, textAlign = TextAlign.Center)
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
                                    onClick = { updateCurrentConfig(currentConfig.copy(applyEffect = tr)) },
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
                                    onClick = { updateCurrentConfig(currentConfig.copy(restoreEffect = tr)) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasUnsavedChanges) Color(0xFF1B2230) else MaterialTheme.colorScheme.surface
                ),
                border = if (hasUnsavedChanges) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hasUnsavedChanges) "Unsaved Modifications" else "Configuration Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else Color.White
                        )
                        if (hasUnsavedChanges) {
                            Text(
                                text = "● Pending Apply",
                                color = Color(0xFFFFB74D),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "✓ Synced",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { applyAndSaveChanges() },
                            enabled = hasUnsavedChanges,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = Color(0xFF282836)
                            )
                        ) {
                            Text(
                                text = if (hasUnsavedChanges) "Save & Apply Changes" else "All Changes Saved",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasUnsavedChanges) Color.Black else Color.Gray
                            )
                        }

                        if (hasUnsavedChanges) {
                            OutlinedButton(
                                onClick = {
                                    localHomeConfig = homeConfig
                                    localLockConfig = lockConfig
                                    localSeparateScreens = separateScreens
                                },
                                modifier = Modifier.height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Discard", fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    try {
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                            putExtra(
                                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                ComponentName(context, AlbumArtWallpaperService::class.java)
                            )
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            val fallback = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                            context.startActivity(fallback)
                        } catch (_: Exception) {
                        }
                    }
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
    config: ScreenConfig,
    progress: Float = 1f,
    mode: Int = 0,
    previous: Bitmap? = null,
    preparedBlur: Bitmap? = null
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
        transitionProgress = progress,
        transitionMode = mode,
        previousBitmap = previous,
        preparedBlur = preparedBlur
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
