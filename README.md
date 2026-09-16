# MPlayerX — Offline Android Video Player

Lightweight, gesture-driven local video player for Android.
MX Player Pro-style UX, hardware-accelerated playback, offline-first, no backend.

## Status: MVP (local APK ready)

Open a local video → plays in under a second, hardware-decoded where the
device supports it (including AV1). See [TODO.md](TODO.md) for milestone tracking.

## Features (MVP scope, requirements §25)

- [x] Open local video (file browser, MediaStore, or `VIEW` intent from any file manager)
- [x] Hardware acceleration first, software fallback; honest decoder reporting
- [x] Play / pause / seek, 0.25×–2× speed, aspect ratio, pinch zoom, fullscreen
- [x] Volume (right swipe) + brightness (left swipe) gestures, double-tap seek, long-press 2×
- [x] Audio track + subtitle track selection, subtitle delay stored, audio delay stored
- [x] Resume playback (always / ask / never), playback history
- [x] File browser: grid/list, search, sort by name/date/size/duration
- [x] Picture-in-Picture, background audio with media notification
- [x] Dark theme
- [ ] libmpv engine (Milestone 2 — see below)

## Gestures

| Gesture | Action |
|---|---|
| Horizontal swipe | Seek backward / forward |
| Left vertical swipe | Brightness down / up |
| Right vertical swipe | Volume down / up |
| Double-tap left / center / right | −10 s / play-pause / +10 s |
| Pinch | Zoom |
| Long press | Temporary 2× speed |

## Stack

| Component | Choice |
|---|---|
| Language / UI | Kotlin + Jetpack Compose |
| Playback (MVP) | Media3 ExoPlayer behind a `PlayerEngine` interface |
| Playback (Milestone 2) | libmpv via NDK/JNI, same interface |
| Discovery | Android MediaStore (`MediaScanner`, IO dispatcher, never main thread) |
| Persistence | Room (history/resume) + DataStore (settings) |
| Thumbnails | Coil |
| DI | Manual (`MPlayerXApp`) — no framework, per requirements §2 |

UI never touches the engine implementation, only `PlayerEngine`
(requirements §6, §23). Swapping in libmpv is a one-line DI change;
the stub `MpvPlayerEngine` already holds the place. Details: [ARCHITECTURE.md](ARCHITECTURE.md).

## Build & install

Prereqs: JDK 17+, Android SDK with `platforms;android-34` + `build-tools;34.0.0`,
Gradle 8.10+. Full notes: [DEVELOPMENT.md](DEVELOPMENT.md).

```powershell
D:\gradle-8.10.2\bin\gradle.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

Push a test file to the device:

```powershell
adb push sample.mp4 /sdcard/Movies/
```

## Project layout

```
app/src/main/java/com/mplayerx/
├── MainActivity.kt  MPlayerXApp.kt      # nav, VIEW intent, manual DI
├── ui/home|browser|player|settings/     # Compose screens
├── playback/                            # PlayerEngine, PlayerState,
│                                        # PlayerController, ExoPlayerEngine,
│                                        # MpvPlayerEngine (stub), PlaybackService
├── media/                               # MediaScanner, MediaRepository, MediaItem
├── database/                            # Room AppDatabase, PlaybackHistory
├── settings/                            # DataStore SettingsRepository
└── utils/Format.kt
```

## Docs

- Product requirements: `Android Video Player — Initial Requirements.md`
- [ARCHITECTURE.md](ARCHITECTURE.md) — engine decision + libmpv swap path
- [DEVELOPMENT.md](DEVELOPMENT.md) — environment, build, install
- [TODO.md](TODO.md) — milestone checklist

## Out of scope for V1

Network playback (HTTP/SMB/DLNA), Android TV, Chromecast, equalizer,
subtitle download, cloud sync, accounts — see requirements §21/§25.
