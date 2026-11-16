# MusWall Lite — Dependency Analysis & Manifest Verification

## 1. Direct Dependency Inventory

| Dependency Coordinate | Version | Purpose & Scope | Overhead / Footprint |
|---|---|---|---|
| `androidx.core:core-ktx` | `1.15.0` | Android KTX extensions & NotificationManagerCompat | Core Android library |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.8.7` | Coroutines lifecycle scope extensions | Low |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.8.7` | Architectural ViewModel integration in Compose | Low |
| `androidx.activity:activity-compose` | `1.9.3` | Activity result contracts & photo picker | Low |
| `androidx.compose:compose-bom` | `2024.10.01` | Bill of Materials pinning Compose components | Zero runtime overhead |
| `androidx.compose.ui:ui` | BOM | Core Compose rendering runtime | Standard UI framework |
| `androidx.compose.ui:ui-graphics` | BOM | Canvas, Color, and Graphics primitives | Standard UI framework |
| `androidx.compose.ui:ui-tooling-preview` | BOM | IDE preview annotations | Stripped in release |
| `androidx.compose.material3:material3` | BOM | Material Design 3 UI components & theming | Standard UI framework |
| `junit:junit` | `4.13.2` | JVM unit testing framework (Test scope) | 0 KB in release |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.9.0` | Coroutine testing utilities (Test scope) | 0 KB in release |

---

## 2. Toolchain & Build Environment

- **Gradle Version:** `8.14`
- **Android Gradle Plugin (AGP):** `8.8.0`
- **Kotlin Compiler:** `2.0.21` (Compose Compiler plugin enabled via `org.jetbrains.kotlin.plugin.compose`)
- **Compile SDK:** `35` (Android 15)
- **Min SDK:** `31` (Android 12)
- **Target SDK:** `35` (Android 15)
- **JVM Target:** `Java 17`

---

## 3. Vulnerability & Footprint Assessment

1. **Third-Party Networking Libraries:** MusWall Lite uses standard `java.net.HttpURLConnection` inside `MusicNotificationListener` for fetching remote artwork URLs. No heavy HTTP clients (OkHttp, Retrofit) are packaged, saving over 3.5 MB of APK size.
2. **Image Loading Libraries:** MusWall Lite implements its own multi-pass box blur engine (`BoxBlurEngine`) and image decoders (`ImageDecoder.ALLOCATOR_SOFTWARE`). Heavy image loading libraries (Glide, Coil) are omitted, saving over 2 MB of APK size and eliminating external caching daemon overhead.
3. **Permissions:**
   - `android.permission.SET_WALLPAPER`: Normal permission for system wallpaper interactions.
   - `android.permission.INTERNET`: Required to download high-resolution cover art URLs from media metadata.
   - `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`: Special system permission required to detect media sessions and playback state.
   - `android.permission.BIND_WALLPAPER`: Special system permission for the live wallpaper service.
   - No location, storage, camera, contacts, or microphone permissions requested.
