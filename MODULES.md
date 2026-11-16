# MusWall Lite — Modules & Component Catalog

## 1. Component Boundaries & Public Contracts

| Module / Component | Package | Primary Responsibility | Public API / Contracts |
|---|---|---|---|
| **ScreenConfig** | `domain.model` | Holds display & effect attributes | Immutable data class with companion constants |
| **WallpaperConfigRepository** | `data.repository` | SharedPreferences storage | `getHomeConfig()`, `getLockConfig()`, `saveHomeConfig()`, `saveLockConfig()`, `isSeparateScreens()`, `setSeparateScreens()` |
| **RestoreImageRepository** | `data.repository` | Disk image I/O & system fallback | `loadHomeRestoreImage()`, `loadLockRestoreImage()`, `saveHomeRestoreImage()`, `saveLockRestoreImage()`, `captureSystemWallpaper()`, `getDefaultArtwork()` |
| **BoxBlurEngine** | `graphics` | Multi-pass box blur | `blur(source: Bitmap, blurPercent: Int): Bitmap` |
| **SmartFramingDetector** | `graphics` | Intelligent horizontal focal framing | `getFocalXPercent(src: Bitmap): Int` |
| **WallpaperRenderer** | `com.muswall.lite` | Canvas rendering & compositing | `render(...)`, `getNormalBlur(...)`, `getFocalXPercent(...)` |
| **WallpaperState** | `com.muswall.lite` | Reactive state orchestration facade | `currentArt`, `isPlaying`, `homeConfig`, `lockConfig`, `separateScreens`, `updateMedia(...)`, `onMediaAppClosed(...)`, `init(...)` |
| **AlbumArtWallpaperService** | `com.muswall.lite` | Android Live Wallpaper service | Standard Android WallpaperService lifecycle |
| **MusicNotificationListener** | `com.muswall.lite` | Intercepts active media sessions | `onListenerConnected()`, `onNotificationPosted()`, `onNotificationRemoved()`, `onActiveSessionsChanged()` |
| **MainViewModel** | `ui.viewmodel` | UI state holding & action handling | `updateHomeConfig()`, `updateLockConfig()`, `setSeparateScreens()`, `applyAndSaveChanges()`, `discardChanges()`, `playPreviewTransition()` |
| **MainScreen** | `com.muswall.lite` | Jetpack Compose dashboard | Composable entrypoint |

---

## 2. Public Interface Contracts

### 2.1 `ScreenConfig`
```kotlin
data class ScreenConfig(
    val effectType: Int = EFFECT_BLUR_STILL,
    val blurPercent: Int = 45,
    val dimPercent: Int = 20,
    val stillScalePercent: Int = 80,
    val stillOffsetYPercent: Int = -35,
    val stillCornerRadiusPercent: Int = 12,
    val applyEffect: Int = TRANSITION_FADE,
    val restoreEffect: Int = TRANSITION_FADE,
    val restoreDelaySeconds: Int = 30,
    val applyEffectsToRestore: Boolean = false,
    val restoreBlurPercent: Int = 45,
    val restoreDimPercent: Int = 20,
    val wallpaperOffsetYPercent: Int = -35,
    val wallpaperOffsetXPercent: Int = 0,
    val autoHorizontalFraming: Boolean = true,
    val aspectFitMode: Int = ASPECT_FIT_WIDTH
)
```

### 2.2 `WallpaperState` Facade
All background services and UI components interact with `WallpaperState` via thread-safe Kotlin StateFlows:
- `currentArt: StateFlow<Bitmap?>`: Currently playing media artwork.
- `isPlaying: StateFlow<Boolean>`: Current playback status.
- `homeConfig: StateFlow<ScreenConfig>`: Persisted Home screen configuration.
- `lockConfig: StateFlow<ScreenConfig>`: Persisted Lock screen configuration.
- `separateScreens: StateFlow<Boolean>`: Toggle for independent home/lock configs.
- `settingsChanged: StateFlow<Long>`: Invalidation clock triggering immediate surface redraws.

---

## 3. Error Handling & Invariants

1. **Bitmap Immutability & Recycling Guards:**
   Before rendering or decoding any bitmap, `!bitmap.isRecycled` is validated. Recycled or null bitmaps trigger a fallback to `RestoreImageRepository.createAmbientGradientArtwork()`.
2. **Low-Memory Defense:**
   All image scaling and blur operations catch `Throwable` (not merely `Exception`) to safeguard against low-RAM `OutOfMemoryError` aborts.
3. **Corrupted File Auto-Purge:**
   If a custom restore wallpaper PNG cannot be decoded by `BitmapFactory`, `RestoreImageRepository` deletes the corrupted file immediately and falls back to ambient gradient art.
