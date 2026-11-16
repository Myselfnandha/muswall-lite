# MusWall Lite — Project Structure Map

## 1. Directory Tree

```
muswall-lite/
├── .github/
│   └── workflows/
│       └── build-release.yml          # GitHub Actions automated release & signing CI/CD
├── app/
│   ├── build.gradle.kts               # App module build script, dependencies & signing configs
│   ├── proguard-rules.pro             # Proguard obfuscation & optimization rules
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml    # Android component declarations, services & permissions
│       │   ├── java/com/muswall/lite/
│       │   │   ├── MainActivity.kt                  # UI entrypoint & screen scaffold
│       │   │   ├── WallpaperState.kt                # Reactive state & orchestration facade
│       │   │   ├── WallpaperRenderer.kt             # Canvas drawing, transitions & frame rendering
│       │   │   ├── AlbumArtWallpaperService.kt      # Android Live Wallpaper engine service
│       │   │   ├── MusicNotificationListener.kt     # Media notification listener & art extraction
│       │   │   ├── domain/
│       │   │   │   └── model/
│       │   │   │       └── ScreenConfig.kt          # Domain entity & configuration specifications
│       │   │   ├── data/
│       │   │   │   └── repository/
│       │   │   │       ├── WallpaperConfigRepository.kt # SharedPreferences persistence
│       │   │   │       └── RestoreImageRepository.kt    # Disk file I/O & system wallpaper capture
│       │   │   ├── graphics/
│       │   │   │   ├── BoxBlurEngine.kt             # High-performance multi-pass box blur
│       │   │   │   └── SmartFramingDetector.kt      # Face detection & visual energy framing
│       │   │   └── ui/
│       │   │       ├── components/                  # Modular UI cards & action components
│       │   │       │   ├── ActionButtonsRow.kt
│       │   │       │   ├── AmbientBlurCard.kt
│       │   │       │   ├── EffectControlsCard.kt
│       │   │       │   ├── FramingCard.kt
│       │   │       │   ├── LivePreviewCard.kt
│       │   │       │   ├── NotificationBanner.kt
│       │   │       │   ├── RecoverWallpaperCard.kt
│       │   │       │   ├── StatusActionCard.kt
│       │   │       │   ├── StillControlsCard.kt
│       │   │       │   ├── TransitionsCard.kt
│       │   │       │   └── WallpaperServiceButtons.kt
│       │   │       ├── theme/
│       │   │       │   └── MusWallLiteTheme.kt      # Jetpack Compose dark theme styling
│       │   │       ├── util/
│       │   │       │   └── PreviewUtils.kt          # Sample art generators & software decoders
│       │   │       └── viewmodel/
│       │   │           └── MainViewModel.kt         # Reactive UI state holder & event dispatcher
│       │   └── res/                                 # Drawables, mipmaps, values, xml configs
│       └── test/
│           └── java/com/muswall/lite/
│               └── domain/
│                   └── model/
│                       └── ScreenConfigTest.kt      # Configuration boundary & default unit tests
├── gradle/
│   └── wrapper/                       # Gradle wrapper binaries & properties (Gradle 8.14)
├── build.gradle.kts                   # Top-level build configuration
├── settings.gradle.kts                # Project module definitions & repository resolution
├── ARCHITECTURE.md                    # Core architecture design & system breakdown
├── PROJECT_STRUCTURE.md               # Repository map & responsibility catalog
├── MODULES.md                         # Module catalog, public APIs & boundaries
├── DATA_FLOW.md                       # End-to-end data flow & state lifecycles
├── DEPENDENCIES.md                    # Dependency analysis & manifest verification
├── DECISIONS.md                       # Architectural Decision Records (ADRs)
└── TESTING.md                         # Testing strategy, verification & coverage
```

---

## 2. Directory Responsibilities

### `com.muswall.lite.domain`
- **Responsibility:** Holds pure business entities, models, and constants that define the behavioral configuration of the live wallpaper and lock screen.
- **Dependencies:** Pure Kotlin data structures with zero Android UI dependencies.
- **Key Files:**
  - `domain/model/ScreenConfig.kt`: Data class defining effect modes, blur/dim parameters, offsets, aspect ratios, transitions, and restore delays.

### `com.muswall.lite.data.repository`
- **Responsibility:** Encapsulates persistence layers, file input/output, hardware/system service integration (WallpaperManager capture), and storage error recovery.
- **Dependencies:** Android Context, SharedPreferences, java.io, android.app.WallpaperManager.
- **Key Files:**
  - `WallpaperConfigRepository.kt`: Stores and retrieves Home and Lock screen preferences using SharedPreferences.
  - `RestoreImageRepository.kt`: Manages saving/loading compressed PNG images from app-private disk storage, extracting system wallpaper, and generating ambient gradient fallbacks.

### `com.muswall.lite.graphics`
- **Responsibility:** High-performance, zero-allocation algorithmic image processing.
- **Dependencies:** Android graphics (`Bitmap`, `Canvas`, `PointF`, `FaceDetector`).
- **Key Files:**
  - `BoxBlurEngine.kt`: Multi-pass box blur with pre-allocated scratch buffers and dynamic downscaling.
  - `SmartFramingDetector.kt`: Face detection and contrast-energy gradient detection for intelligent vertical phone cropping.

### `com.muswall.lite.ui`
- **Responsibility:** User configuration interface built in declarative Jetpack Compose, featuring real-time preview simulation, parameter sliders, image pickers, and service status monitors.
- **Dependencies:** Compose UI, Material3, ViewModel Compose, Coroutines.
- **Key Subpackages:**
  - `ui/components/`: Granular, reusable cards adhering to Single Responsibility Principle.
  - `ui/viewmodel/`: State holder (`MainViewModel`) isolating UI side effects and managing unsaved states.
  - `ui/util/`: Bitmap decoders and mock media preview generators.
  - `ui/theme/`: Dark mode color palettes, typography, and elevation rules.

### Root Package (`com.muswall.lite`)
- **Responsibility:** Android OS integrations, wallpaper lifecycle management, and reactive coordination facades.
- **Key Files:**
  - `MainActivity.kt`: Hosts Compose hierarchy and forwards permission/crop activity intents.
  - `WallpaperState.kt`: Reactive facade orchestrating playback changes, restore timers, and background blur precomputations.
  - `WallpaperRenderer.kt`: Drawing pipeline executing transitions, aspect framing, scaling, and canvas drawing.
  - `AlbumArtWallpaperService.kt`: Live Wallpaper engine receiving system ticks, visibility updates, and lockscreen flags.
  - `MusicNotificationListener.kt`: MediaSession and notification hook extracting album artwork across Spotify, YouTube Music, Apple Music, and local players.
