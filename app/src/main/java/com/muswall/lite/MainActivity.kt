package com.muswall.lite

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
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
            if (localSeparateScreens) {
                if (activeTab == 1) {
                    WallpaperState.setLockRestoreBitmap(context, bmp)
                } else {
                    WallpaperState.setHomeRestoreBitmap(context, bmp)
                }
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
    val sampleHomeRestore = remember { generateSampleRestoreArtwork(false) }
    val sampleLockRestore = remember { generateSampleRestoreArtwork(true) }

    val displayArt = currentArt ?: sampleArt
    val effectiveHomeRestore = homeRestoreArt ?: sampleHomeRestore
    val effectiveLockRestore = if (localSeparateScreens) (lockRestoreArt ?: sampleLockRestore) else effectiveHomeRestore

    val currentConfig = if (localSeparateScreens && activeTab == 1) localLockConfig else localHomeConfig

    fun updateHomeConfig(newConfig: ScreenConfig) {
        localHomeConfig = newConfig
        WallpaperState.updateHomeConfig(context, newConfig)
        if (!localSeparateScreens) {
            localLockConfig = newConfig
            WallpaperState.updateLockConfig(context, newConfig)
        }
    }

    fun updateLockConfig(newConfig: ScreenConfig) {
        localLockConfig = newConfig
        WallpaperState.updateLockConfig(context, newConfig)
    }

    fun updateCurrentConfig(newConfig: ScreenConfig) {
        if (localSeparateScreens && activeTab == 1) {
            updateLockConfig(newConfig)
        } else {
            updateHomeConfig(newConfig)
        }
    }

    val homeRestoreBlur = remember(
        effectiveHomeRestore,
        localHomeConfig.restoreBlurPercent,
        localHomeConfig.applyEffectsToRestore
    ) {
        if (localHomeConfig.applyEffectsToRestore && localHomeConfig.restoreBlurPercent > 0) {
            WallpaperRenderer.getNormalBlur(effectiveHomeRestore, localHomeConfig.restoreBlurPercent)
        } else null
    }

    val homeMusicBlur = remember(
        displayArt,
        localHomeConfig.blurPercent,
        localHomeConfig.effectType
    ) {
        if (localHomeConfig.effectType != ScreenConfig.EFFECT_STILL && localHomeConfig.blurPercent > 0) {
            WallpaperRenderer.getNormalBlur(displayArt, localHomeConfig.blurPercent)
        } else null
    }

    val homePreviewBitmap = remember(
        displayArt,
        effectiveHomeRestore,
        previewShowingRestore,
        localHomeConfig,
        previewAnimProgress,
        homeMusicBlur,
        homeRestoreBlur
    ) {
        val mode = if (previewShowingRestore) localHomeConfig.restoreEffect else localHomeConfig.applyEffect
        val previous = if (previewShowingRestore) displayArt else effectiveHomeRestore
        renderScreenPreview(
            art = displayArt,
            restore = effectiveHomeRestore,
            isRestore = previewShowingRestore,
            config = localHomeConfig,
            progress = previewAnimProgress,
            mode = mode,
            previous = previous,
            preparedBlur = homeMusicBlur,
            preparedRestoreBlur = homeRestoreBlur
        )
    }

    val effectiveLockConfig = if (localSeparateScreens) localLockConfig else localHomeConfig

    val lockRestoreBlur = remember(
        effectiveLockRestore,
        effectiveLockConfig.restoreBlurPercent,
        effectiveLockConfig.applyEffectsToRestore
    ) {
        if (effectiveLockConfig.applyEffectsToRestore && effectiveLockConfig.restoreBlurPercent > 0) {
            WallpaperRenderer.getNormalBlur(effectiveLockRestore, effectiveLockConfig.restoreBlurPercent)
        } else null
    }

    val lockMusicBlur = remember(
        displayArt,
        effectiveLockConfig.blurPercent,
        effectiveLockConfig.effectType
    ) {
        if (effectiveLockConfig.effectType != ScreenConfig.EFFECT_STILL && effectiveLockConfig.blurPercent > 0) {
            WallpaperRenderer.getNormalBlur(displayArt, effectiveLockConfig.blurPercent)
        } else null
    }

    val lockPreviewBitmap = remember(
        displayArt,
        effectiveLockRestore,
        previewShowingRestore,
        effectiveLockConfig,
        previewAnimProgress,
        lockMusicBlur,
        lockRestoreBlur
    ) {
        val mode = if (previewShowingRestore) effectiveLockConfig.restoreEffect else effectiveLockConfig.applyEffect
        val previous = if (previewShowingRestore) displayArt else effectiveLockRestore
        renderScreenPreview(
            art = displayArt,
            restore = effectiveLockRestore,
            isRestore = previewShowingRestore,
            config = effectiveLockConfig,
            progress = previewAnimProgress,
            mode = mode,
            previous = previous,
            preparedBlur = lockMusicBlur,
            preparedRestoreBlur = lockRestoreBlur
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
                            onCheckedChange = {
                                localSeparateScreens = it
                                WallpaperState.setSeparateScreens(context, it)
                            },
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
                                text = if (previewShowingRestore) "Restored Default Wallpaper (Home & Lock)" else (trackTitle ?: "Music Album Artwork"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !previewShowingRestore,
                                onClick = { playPreviewTransition(false) },
                                label = { Text("🎵 Music", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = previewShowingRestore,
                                onClick = { playPreviewTransition(true) },
                                label = { Text("🖼️ Recover", fontSize = 11.sp) }
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
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 10.dp)
                                            .width(55.dp)
                                            .height(18.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0x35FFFFFF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("12:45", fontSize = 8.sp, color = Color(0x99FFFFFF))
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 6.dp)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0x30FFFFFF))
                                        )

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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Wallpaper Effect",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Choose how music artwork is displayed on ${if (localSeparateScreens && activeTab == 1) "Lock Screen" else "Home Screen"}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentConfig.effectType == ScreenConfig.EFFECT_BLUR,
                            onClick = { updateCurrentConfig(currentConfig.copy(effectType = ScreenConfig.EFFECT_BLUR)) },
                            label = { Text("Blur", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentConfig.effectType == ScreenConfig.EFFECT_STILL,
                            onClick = { updateCurrentConfig(currentConfig.copy(effectType = ScreenConfig.EFFECT_STILL)) },
                            label = { Text("Still", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentConfig.effectType == ScreenConfig.EFFECT_BLUR_STILL,
                            onClick = { updateCurrentConfig(currentConfig.copy(effectType = ScreenConfig.EFFECT_BLUR_STILL)) },
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
                                onClick = { updateCurrentConfig(currentConfig.copy(aspectFitMode = ScreenConfig.ASPECT_FIT_WIDTH)) },
                                label = { Text("🖼️ Fit Width (Ambient)", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = currentConfig.aspectFitMode == ScreenConfig.ASPECT_FILL,
                                onClick = { updateCurrentConfig(currentConfig.copy(aspectFitMode = ScreenConfig.ASPECT_FILL)) },
                                label = { Text("📐 Full Bleed (Fill)", fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (currentConfig.effectType != ScreenConfig.EFFECT_BLUR) {
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
                            text = "Music Still Artwork Controls",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        Column {
                            val safeZoneOffset = if (localSeparateScreens && activeTab == 1) 35 else -35
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
                                                updateCurrentConfig(currentConfig.copy(stillOffsetYPercent = safeZoneOffset))
                                            }
                                        )
                                    }
                                }
                            }
                            Slider(
                                value = currentConfig.stillOffsetYPercent.toFloat(),
                                onValueChange = {
                                    updateCurrentConfig(currentConfig.copy(stillOffsetYPercent = it.toInt()))
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
                                                updateCurrentConfig(currentConfig.copy(stillOffsetYPercent = value))
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
                                    updateCurrentConfig(currentConfig.copy(stillScalePercent = it.toInt()))
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
                                    updateCurrentConfig(currentConfig.copy(stillCornerRadiusPercent = it.toInt()))
                                },
                                valueRange = 0f..50f
                            )
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
                                    updateCurrentConfig(currentConfig.copy(blurPercent = it.toInt()))
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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Wallpaper Framing & Alignment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Adjust framing and alignment of cover wallpaper on ${if (localSeparateScreens && activeTab == 1) "Lock Screen" else "Home Screen"}",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    if (currentConfig.effectType == ScreenConfig.EFFECT_BLUR && currentConfig.aspectFitMode == ScreenConfig.ASPECT_FILL) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Horizontal Framing", fontSize = 13.sp)
                                val xAlignLabel = when {
                                    currentConfig.wallpaperOffsetXPercent == 0 -> "Center (0%)"
                                    currentConfig.wallpaperOffsetXPercent <= -70 -> "Left Focus (${currentConfig.wallpaperOffsetXPercent}%)"
                                    currentConfig.wallpaperOffsetXPercent >= 70 -> "Right Focus (+${currentConfig.wallpaperOffsetXPercent}%)"
                                    currentConfig.wallpaperOffsetXPercent < 0 -> "Left ${-currentConfig.wallpaperOffsetXPercent}%"
                                    else -> "Right +${currentConfig.wallpaperOffsetXPercent}%"
                                }
                                Text(xAlignLabel, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                            Slider(
                                value = currentConfig.wallpaperOffsetXPercent.toFloat(),
                                onValueChange = {
                                    updateCurrentConfig(currentConfig.copy(wallpaperOffsetXPercent = it.toInt()))
                                },
                                valueRange = -100f..100f
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Left Focus" to -80,
                                    "Center" to 0,
                                    "Right Focus" to 80
                                ).forEach { (label, value) ->
                                    val isSelected = currentConfig.wallpaperOffsetXPercent == value
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                updateCurrentConfig(currentConfig.copy(wallpaperOffsetXPercent = value))
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

                    Column {
                        val safeZoneOffset = if (localSeparateScreens && activeTab == 1) 35 else -35
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
                                updateCurrentConfig(currentConfig.copy(wallpaperOffsetYPercent = it.toInt()))
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
                                            updateCurrentConfig(currentConfig.copy(wallpaperOffsetYPercent = value))
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

            var recoverTab by remember { mutableIntStateOf(0) }
            if (!localSeparateScreens && recoverTab == 1) {
                recoverTab = 0
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recover Wallpaper (Default)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                text = if (localSeparateScreens) "Separate wallpaper & effects for each screen" else "Wallpaper restored when playback stops",
                                fontSize = 12.sp,
                                color = Color.LightGray
                            )
                        }
                        FilledTonalButton(
                            onClick = { playPreviewTransition(!previewShowingRestore) },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (previewShowingRestore) "🎵 Live Music" else "🖼️ Live Recover",
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (localSeparateScreens) {
                        TabRow(
                            selectedTabIndex = recoverTab,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        ) {
                            Tab(
                                selected = recoverTab == 0,
                                onClick = {
                                    recoverTab = 0
                                    activeTab = 0
                                },
                                text = { Text("🏠 Home Recover", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                            )
                            Tab(
                                selected = recoverTab == 1,
                                onClick = {
                                    recoverTab = 1
                                    activeTab = 1
                                },
                                text = { Text("🔒 Lock Recover", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
                            )
                        }
                    }

                    val activeRecoverConfig = if (localSeparateScreens && recoverTab == 1) localLockConfig else localHomeConfig
                    val onRecoverConfigChange: (ScreenConfig) -> Unit = { updated ->
                        if (localSeparateScreens && recoverTab == 1) {
                            updateLockConfig(updated)
                        } else {
                            updateHomeConfig(updated)
                        }
                    }
                    val screenTitle = if (localSeparateScreens) (if (recoverTab == 1) "Lock Screen" else "Home Screen") else "All Screens"
                    val currentRestoreArt = if (localSeparateScreens && recoverTab == 1) (lockRestoreArt ?: effectiveLockRestore) else (homeRestoreArt ?: effectiveHomeRestore)
                    val isCustomArt = if (localSeparateScreens && recoverTab == 1) (lockRestoreArt != null) else (homeRestoreArt != null)

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
                                .clickable {
                                    if (localSeparateScreens && recoverTab == 1) {
                                        lockPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    } else {
                                        homePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                },
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
                                onClick = {
                                    if (localSeparateScreens && recoverTab == 1) {
                                        lockPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    } else {
                                        homePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                },
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
                                if (it) previewShowingRestore = true
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
                                    if (!previewShowingRestore) previewShowingRestore = true
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
                                    if (!previewShowingRestore) previewShowingRestore = true
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
                            onClick = {
                                if (localSeparateScreens && recoverTab == 1) {
                                    lockPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    homePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Notification Access", fontSize = 12.sp, textAlign = TextAlign.Center)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("App Info (Restricted / Battery)", fontSize = 11.sp, textAlign = TextAlign.Center)
                }
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
    preparedBlur: Bitmap? = null,
    preparedRestoreBlur: Bitmap? = null
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
        preparedBlur = preparedBlur,
        preparedRestoreBlur = preparedRestoreBlur,
        xOffset = 0.5f,
        displayWidth = pw
    )
    return bmp
}

fun loadSoftwareBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = true
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

fun generateSampleRestoreArtwork(isLock: Boolean): Bitmap {
    val w = 540
    val h = 1200
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val colors = if (isLock) {
        intArrayOf(
            AndroidColor.parseColor("#0F2027"),
            AndroidColor.parseColor("#203A43"),
            AndroidColor.parseColor("#2C5364")
        )
    } else {
        intArrayOf(
            AndroidColor.parseColor("#2E1065"),
            AndroidColor.parseColor("#701A75"),
            AndroidColor.parseColor("#9D174D"),
            AndroidColor.parseColor("#EA580C")
        )
    }

    val gradient = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        colors,
        null,
        Shader.TileMode.CLAMP
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = gradient
    }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

    val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(40, 255, 255, 255)
        style = Paint.Style.FILL
    }
    canvas.drawCircle(w * 0.5f, h * 0.4f, w * 0.35f, shapePaint)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(200, 255, 255, 255)
        textSize = 34f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText(if (isLock) "LOCK DEFAULT" else "HOME DEFAULT", w * 0.5f, h * 0.41f, labelPaint)

    return bmp
}
