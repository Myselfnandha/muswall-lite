package com.muswall.lite

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.muswall.lite.domain.model.ScreenConfig
import com.muswall.lite.ui.components.*
import com.muswall.lite.ui.theme.MusWallLiteTheme
import com.muswall.lite.ui.util.PreviewUtils
import com.muswall.lite.ui.viewmodel.MainViewModel
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WallpaperState.init(applicationContext)
        viewModel.refreshStatus(this)

        setContent {
            MusWallLiteTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (WallpaperState.homeRestoreArt.value == null) {
            WallpaperState.captureSystemDefaultWallpaper(applicationContext)
        }
        viewModel.refreshStatus(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    val homeConfig by WallpaperState.homeConfig.collectAsState()
    val lockConfig by WallpaperState.lockConfig.collectAsState()
    val separateScreens by WallpaperState.separateScreens.collectAsState()

    val localHomeConfig by viewModel.localHomeConfig.collectAsState()
    val localLockConfig by viewModel.localLockConfig.collectAsState()
    val localSeparateScreens by viewModel.localSeparateScreens.collectAsState()

    val activeTab by viewModel.activeTab.collectAsState()
    val recoverTab by viewModel.recoverTab.collectAsState()
    val previewShowingRestore by viewModel.previewShowingRestore.collectAsState()
    val previewAnimProgress by viewModel.previewAnimProgress.collectAsState()

    val isNotificationGranted by viewModel.isNotificationGranted.collectAsState()
    val isWallpaperActive by viewModel.isWallpaperActive.collectAsState()

    val hasUnsavedChanges = (localHomeConfig != homeConfig || localLockConfig != lockConfig || localSeparateScreens != separateScreens)

    val currentArt by WallpaperState.currentArt.collectAsState()
    val homeRestoreArt by WallpaperState.homeRestoreArt.collectAsState()
    val lockRestoreArt by WallpaperState.lockRestoreArt.collectAsState()
    val trackTitle by WallpaperState.trackTitle.collectAsState()

    val sampleArt = remember { PreviewUtils.generateSampleArtwork() }
    val sampleHomeRestore = remember { WallpaperState.getDefaultRestoreArt(false) }
    val sampleLockRestore = remember { WallpaperState.getDefaultRestoreArt(true) }

    val displayArt = currentArt ?: sampleArt
    val effectiveHomeRestore = homeRestoreArt ?: sampleHomeRestore
    val effectiveLockRestore = if (localSeparateScreens) (lockRestoreArt ?: sampleLockRestore) else effectiveHomeRestore

    val homeRestoreBlur = remember(effectiveHomeRestore, localHomeConfig.restoreBlurPercent, localHomeConfig.applyEffectsToRestore) {
        if (localHomeConfig.applyEffectsToRestore && localHomeConfig.restoreBlurPercent > 0) {
            WallpaperRenderer.getNormalBlur(effectiveHomeRestore, localHomeConfig.restoreBlurPercent)
        } else null
    }

    val homeMusicBlur = remember(displayArt, localHomeConfig.blurPercent, localHomeConfig.effectType) {
        if (localHomeConfig.effectType != ScreenConfig.EFFECT_STILL && localHomeConfig.blurPercent > 0) {
            WallpaperRenderer.getNormalBlur(displayArt, localHomeConfig.blurPercent)
        } else null
    }

    val homePreviewBitmap = remember(
        displayArt, effectiveHomeRestore, previewShowingRestore, localHomeConfig, previewAnimProgress, homeMusicBlur, homeRestoreBlur
    ) {
        val mode = if (previewShowingRestore) localHomeConfig.restoreEffect else localHomeConfig.applyEffect
        val previous = if (previewShowingRestore) displayArt else effectiveHomeRestore
        PreviewUtils.renderScreenPreview(
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

    val lockRestoreBlur = remember(effectiveLockRestore, effectiveLockConfig.restoreBlurPercent, effectiveLockConfig.applyEffectsToRestore) {
        if (effectiveLockConfig.applyEffectsToRestore && effectiveLockConfig.restoreBlurPercent > 0) {
            WallpaperRenderer.getNormalBlur(effectiveLockRestore, effectiveLockConfig.restoreBlurPercent)
        } else null
    }

    val lockMusicBlur = remember(displayArt, effectiveLockConfig.blurPercent, effectiveLockConfig.effectType) {
        if (effectiveLockConfig.effectType != ScreenConfig.EFFECT_STILL && effectiveLockConfig.blurPercent > 0) {
            WallpaperRenderer.getNormalBlur(displayArt, effectiveLockConfig.blurPercent)
        } else null
    }

    val lockPreviewBitmap = remember(
        displayArt, effectiveLockRestore, previewShowingRestore, effectiveLockConfig, previewAnimProgress, lockMusicBlur, lockRestoreBlur
    ) {
        val mode = if (previewShowingRestore) effectiveLockConfig.restoreEffect else effectiveLockConfig.applyEffect
        val previous = if (previewShowingRestore) displayArt else effectiveLockRestore
        PreviewUtils.renderScreenPreview(
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

    val cropWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        WallpaperState.captureSystemDefaultWallpaper(context)
    }

    fun launchSystemDimensionSetter(sourceUri: Uri, isLock: Boolean) {
        viewModel.handleImagePicked(context, sourceUri, isLock)
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
        if (uri != null) launchSystemDimensionSetter(uri, false)
    }

    val lockPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) launchSystemDimensionSetter(uri, true)
    }

    val currentConfig = if (localSeparateScreens && activeTab == 1) localLockConfig else localHomeConfig
    val safeZoneOffset = if (localSeparateScreens && activeTab == 1) 35 else -35
    val currentTabLabel = if (localSeparateScreens && activeTab == 1) "Lock Screen" else "Home Screen"

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
                                onClick = {
                                    viewModel.applyAndSaveChanges(context)
                                    Toast.makeText(context, "Changes saved and applied!", Toast.LENGTH_SHORT).show()
                                },
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
                            onCheckedChange = { viewModel.setSeparateScreens(context, it) },
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
        ) {
            NotificationBanner(
                isNotificationGranted = isNotificationGranted,
                onGrantClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
            )

            LivePreviewCard(
                trackTitle = trackTitle,
                previewShowingRestore = previewShowingRestore,
                homePreviewBitmap = homePreviewBitmap,
                lockPreviewBitmap = lockPreviewBitmap,
                activeTab = activeTab,
                onTabSelect = { viewModel.setActiveTab(it) },
                onTogglePreviewRestore = { viewModel.playPreviewTransition(it) }
            )

            if (localSeparateScreens) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { viewModel.setActiveTab(0) },
                        text = { Text("🏠 Home Screen Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { viewModel.setActiveTab(1) },
                        text = { Text("🔒 Lock Screen Settings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EffectControlsCard(
                    currentConfig = currentConfig,
                    screenLabel = currentTabLabel,
                    onConfigChange = { viewModel.updateCurrentConfig(context, it) }
                )

                StillControlsCard(
                    currentConfig = currentConfig,
                    safeZoneOffset = safeZoneOffset,
                    onConfigChange = { viewModel.updateCurrentConfig(context, it) }
                )

                AmbientBlurCard(
                    currentConfig = currentConfig,
                    onConfigChange = { viewModel.updateCurrentConfig(context, it) }
                )

                FramingCard(
                    currentConfig = currentConfig,
                    screenLabel = currentTabLabel,
                    activeArt = if (previewShowingRestore) (if (localSeparateScreens && activeTab == 1) effectiveLockRestore else effectiveHomeRestore) else displayArt,
                    safeZoneOffset = safeZoneOffset,
                    onConfigChange = { viewModel.updateCurrentConfig(context, it) }
                )

                val activeRecoverConfig = if (localSeparateScreens && recoverTab == 1) localLockConfig else localHomeConfig
                val currentRestoreArt = if (localSeparateScreens && recoverTab == 1) (lockRestoreArt ?: effectiveLockRestore) else (homeRestoreArt ?: effectiveHomeRestore)
                val isCustomArt = if (localSeparateScreens && recoverTab == 1) (lockRestoreArt != null) else (homeRestoreArt != null)

                RecoverWallpaperCard(
                    separateScreens = localSeparateScreens,
                    recoverTab = recoverTab,
                    onRecoverTabChange = {
                        viewModel.setRecoverTab(it)
                        viewModel.setActiveTab(it)
                    },
                    activeRecoverConfig = activeRecoverConfig,
                    currentRestoreArt = currentRestoreArt,
                    isCustomArt = isCustomArt,
                    previewShowingRestore = previewShowingRestore,
                    onTogglePreviewRestore = { viewModel.playPreviewTransition(!previewShowingRestore) },
                    onPickImage = {
                        if (localSeparateScreens && recoverTab == 1) {
                            lockPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            homePhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                    onCaptureSystem = { WallpaperState.captureSystemDefaultWallpaper(context) },
                    onOpenSystemWallpaperChooser = {
                        try {
                            val intent = Intent(Intent.ACTION_SET_WALLPAPER)
                            context.startActivity(Intent.createChooser(intent, "System Wallpaper Setter"))
                        } catch (_: Exception) {}
                    },
                    onRecoverConfigChange = { updated ->
                        if (localSeparateScreens && recoverTab == 1) {
                            viewModel.updateLockConfig(context, updated)
                        } else {
                            viewModel.updateHomeConfig(context, updated)
                        }
                    }
                )

                TransitionsCard(
                    currentConfig = currentConfig,
                    onConfigChange = { viewModel.updateCurrentConfig(context, it) }
                )

                StatusActionCard(
                    hasUnsavedChanges = hasUnsavedChanges,
                    onSave = {
                        viewModel.applyAndSaveChanges(context)
                        Toast.makeText(context, "Changes saved and applied!", Toast.LENGTH_SHORT).show()
                    },
                    onDiscard = { viewModel.discardChanges() }
                )

                WallpaperServiceButtons(
                    isWallpaperActive = isWallpaperActive,
                    isNotificationGranted = isNotificationGranted,
                    onSetWallpaperClick = {
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
                            } catch (_: Exception) {}
                        }
                    },
                    onNotificationSettingsClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onAppInfoClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                )
            }
        }
    }
}
