# MusWall Lite 🎵

An ultra-lightweight, battery-efficient live wallpaper for Android that transforms your current playing music album artwork into a dynamic, aesthetic background.

Replicated from MusWall using the [replica-skill](https://github.com/Jakeschincariol/replica-skill) philosophy: **zero ads, zero tracking SDKs, zero background CPU wake-locks, and under 20MB RAM usage** (vs ~200MB in the original).

---

## ✨ Features

- **Full Bleed Edge-to-Edge Cover Art**: High-resolution, center-cropped album art scaled to your device's screen.
- **Customizable Gaussian Blur**: Smooth, hardware-accelerated blur slider (0% to 100%).
- **Vignette & Dimming**: Adjustable overlay darkness for clear home-screen icon visibility.
- **Pure Event-Driven Lifecycle**: Draws directly onto the `SurfaceHolder` Canvas **only** on song/metadata changes. Idles at **0% CPU** when listening or paused.
- **Privacy First**: No telemetry, no AdMob, no analytics. Works offline.

---

## 🚀 GitHub Actions Automated Build

Every push to `main` builds the APK artifact automatically. Pushing a version tag (e.g. `v1.0.0`) automatically publishes a GitHub Release with the compiled APK attached.

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 🛠 Manual Local Build

Requirements:
- Android SDK (API 31 - 35)
- Java 17

```bash
chmod +x gradlew
./gradlew assembleRelease
```

The APK will be available at:
`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 📱 Granting Notification Listener via ADB

If you don't want to enable notification listener manually through Android Settings:

```bash
adb shell cmd notification allow_listener com.muswall.lite/.MusicNotificationListener
```

---

## 📄 License
MIT License
