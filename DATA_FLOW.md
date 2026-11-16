# MusWall Lite — Data Flow & State Lifecycle

## 1. Media Playback Event Lifecycle

```mermaid
sequenceDiagram
    autonumber
    participant App as Music Player (Spotify / YT Music)
    participant Listener as MusicNotificationListener
    participant State as WallpaperState
    participant Blur as BoxBlurEngine
    participant Service as AlbumArtWallpaperService
    participant Renderer as WallpaperRenderer
    participant Surface as Canvas / SurfaceHolder

    App->>Listener: Broadcast MediaSession metadata / Notification
    Listener->>Listener: Extract Bitmap (Tiered: MediaMetadata -> Notification Extra -> HTTP stream)
    Listener->>State: updateMedia(art, isPlaying=true, title)
    State->>State: Cancel active restore delay jobs
    State->>Blur: schedulePrecomputeBlur() on Dispatchers.Default
    Blur-->>State: preparedHomeBlur / preparedLockBlur ready
    State->>Service: Flow emission (currentArt, isRestoreActive=false)
    Service->>Renderer: render(canvas, art, config, transitionProgress)
    Renderer->>Surface: Draw composed frame to screen
```

---

## 2. Media Pause & Delay Restore Flow

When playback pauses:
1. `MusicNotificationListener` observes `PlaybackState.STATE_PAUSED` or `STATE_STOPPED`.
2. Listener invokes `WallpaperState.updateMedia(art, isPlaying=false, title)`.
3. If `config.restoreDelaySeconds == 0`, restore wallpaper is activated immediately.
4. If `config.restoreDelaySeconds > 0`, a coroutine delay job (`homeRestoreJob` / `lockRestoreJob`) is scheduled.
5. If music resumes before the delay expires, the restore job is cancelled immediately.
6. Upon delay expiration, `isHomeRestoreActive` / `isLockRestoreActive` flips to `true`, and the wallpaper smoothly transitions back to the default/restored wallpaper.

---

## 3. Media App Closed / Terminated Flow

When the user swipes away the music player:
1. `MusicNotificationListener.onNotificationRemoved()` or `onSessionDestroyed()` is triggered.
2. Listener detects zero active media sessions and calls `WallpaperState.onMediaAppClosed(context)`.
3. Any active 30s delay timers are **immediately cancelled**.
4. `currentArt` is cleared to `null` and `isHomeRestoreActive` is set to `true`.
5. `AlbumArtWallpaperService` immediately renders a smooth transition back to the default wallpaper without delay.

---

## 4. UI Configuration Flow

```mermaid
flowchart LR
    User[User adjusts Slider in Compose UI] --> VM[MainViewModel.updateCurrentConfig]
    VM --> LivePreview[LivePreviewCard recomputes local preview bitmap]
    VM --> Save[User taps Save & Apply]
    Save --> Repo[WallpaperConfigRepository.saveConfig]
    Save --> State[WallpaperState.notifySettingsChanged]
    State --> Blur[BoxBlurEngine precomputes blur]
    State --> Service[AlbumArtWallpaperService receives update]
    Service --> Draw[WallpaperRenderer re-renders live wallpaper]
```

---

## 5. System Default Wallpaper Capture & Fallback

```mermaid
flowchart TD
    Init[App Startup / Restore Image Request] --> CheckFiles{Custom Restore PNG exists?}
    CheckFiles -->|Yes| LoadDisk[BitmapFactory.decodeFile]
    CheckFiles -->|No| CheckPerm{WallpaperManager permission granted?}
    CheckPerm -->|Yes| Capture[WallpaperManager.builtInDrawable / drawable]
    Capture --> ValidBmp{Valid Drawable?}
    ValidBmp -->|Yes| SaveDisk[Compress & Save PNG to filesDir]
    ValidBmp -->|No| Fallback[RestoreImageRepository.createAmbientGradientArtwork]
    CheckPerm -->|No / SecurityException| Fallback
    Fallback --> CanvasDraw[LinearGradient Canvas Composition]
```
