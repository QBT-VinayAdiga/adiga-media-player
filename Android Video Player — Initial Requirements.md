# Android Video Player — Initial Requirements

**Working Name:** MPlayerX  
**Platform:** Android only  
**Status:** Initial Product Requirements  
**Primary Goal:** Build a lightweight, extremely responsive Android video player with an MX Player Pro-style user experience and mpv/libmpv playback capabilities.

---

## 1. Product Vision

Build a native Android video player that combines:

- A polished, simple, gesture-driven UX inspired by MX Player Pro
- The powerful playback capabilities of mpv/libmpv
- Hardware-accelerated playback wherever supported by the device
- Strong AV1 support
- Very fast startup
- Low memory usage
- Smooth 60/120 FPS UI
- Offline-first operation
- No backend dependency for core playback

The application should primarily function as a **local/offline video player**.

---

# 2. Technology Stack

| Component | Technology | Reason |
|---|---|---|
| Language | **Kotlin** | Native Android and modern language |
| UI | **Jetpack Compose** | Modern native UI with fast development |
| Playback Engine | **libmpv** | Powerful and mature media engine |
| Native Integration | **Android NDK + JNI** | Kotlin ↔ native/libmpv integration |
| Database | **Room / SQLite** | Playback history and metadata |
| Media Discovery | **Android MediaStore** | Native media indexing |
| Image Loading | **Coil** | Efficient thumbnail loading |
| Async | **Kotlin Coroutines** | Native asynchronous programming |
| Preferences | **DataStore** | Lightweight persistent settings |
| Build | **Gradle + Kotlin DSL** | Standard Android build |
| Dependency Injection | Hilt or lightweight manual DI | Keep architecture manageable |

## Recommended Stack

> **Kotlin + Jetpack Compose + Android NDK/JNI + libmpv**

Avoid React Native, Flutter, WebView, or other cross-platform frameworks for the initial version.

The application is specifically optimized for Android, so the playback and rendering pipeline should remain as close to native Android as possible.

---

# 3. High-Level Architecture

```text
┌─────────────────────────────────────┐
│           Jetpack Compose           │
│                                     │
│  Home │ Browser │ Player │ Settings │
└──────────────────┬──────────────────┘
                   │
                   ▼
          ┌─────────────────┐
          │ PlayerController│
          └────────┬────────┘
                   │
                   ▼
          ┌─────────────────┐
          │  PlayerEngine   │
          │    Interface    │
          └────────┬────────┘
                   │
                   ▼
          ┌─────────────────┐
          │ MpvPlayerEngine │
          └────────┬────────┘
                   │
                   ▼
                JNI / NDK
                   │
                   ▼
               ┌───────┐
               │ libmpv│
               └───┬───┘
                   │
          ┌────────┴────────┐
          ▼                 ▼
    HW Decoder / GPU      Audio
```

The UI must **not directly depend on libmpv**.

---

# 4. Project Structure

```text
app/
│
├── ui/
│   ├── home/
│   ├── player/
│   ├── browser/
│   └── settings/
│
├── playback/
│   ├── PlayerEngine.kt
│   ├── PlayerController.kt
│   ├── PlayerState.kt
│   └── MpvPlayerEngine.kt
│
├── media/
│   ├── MediaScanner.kt
│   ├── MediaRepository.kt
│   └── MediaItem.kt
│
├── database/
│   ├── AppDatabase.kt
│   └── PlaybackHistory.kt
│
├── settings/
│
└── native/
    ├── JNI
    └── libmpv
```

---

# 5. Performance Requirements

Performance is a core product requirement.

## 5.1 Startup

Target:

- Application launch: **<500 ms** on modern devices
- Local video playback start: **<1 second** where practical
- Avoid unnecessary loading screens
- Avoid expensive initialization during application startup

## 5.2 UI

Target:

- Minimum 60 FPS
- Support 120 FPS devices
- Smooth scrolling
- Instant gesture response
- No blocking operations on the main thread

## 5.3 Memory

The application must not load entire video files into memory.

Incorrect:

```text
2 GB video
    ↓
Read entire file
    ↓
RAM
```

Expected:

```text
Video
  ↓
Streaming / buffering
  ↓
libmpv
  ↓
Decoder
  ↓
GPU / Display
```

## 5.4 Dependencies

Minimize third-party dependencies.

Every dependency should have a clear justification.

---

# 6. Playback Engine

libmpv will be the primary playback engine.

The Android layer should expose an abstraction similar to:

```kotlin
interface PlayerEngine {

    fun open(uri: Uri)

    fun play()
    fun pause()

    fun seek(positionMs: Long)

    fun setVolume(volume: Float)

    fun setPlaybackSpeed(speed: Float)

    fun selectAudioTrack(id: Int)

    fun selectSubtitleTrack(id: Int)

    fun setSubtitleDelay(delayMs: Long)

    fun setVideoScale(scale: Float)

    fun setAspectRatio(ratio: String)

    fun release()
}
```

The exact API can evolve during implementation.

The important requirement is that the rest of the application should depend on `PlayerEngine`, not directly on libmpv.

---

# 7. Supported Video Formats

## 7.1 Video Codecs

Initial targets:

- H.264 / AVC
- H.265 / HEVC
- AV1
- VP8
- VP9
- MPEG-4
- MPEG-2

## 7.2 Containers

Initial targets:

- MP4
- MKV
- WebM
- AVI
- MOV
- TS
- M2TS

## 7.3 Audio

Initial targets:

- AAC
- MP3
- Opus
- Vorbis
- FLAC
- AC3
- E-AC3
- DTS where supported by the underlying device/playback pipeline

Actual hardware decoding and audio capabilities will depend on the Android device.

---

# 8. Hardware Acceleration

Hardware acceleration should be a first-class feature.

Requirements:

1. Prefer hardware decoding when available.
2. Fall back to software decoding when hardware decoding is unavailable.
3. Avoid unnecessary CPU-based processing.
4. Use GPU-based rendering where appropriate.
5. Provide an advanced playback information screen.

Example:

```text
Playback Information

Video
────────────────────────
Codec       AV1
Resolution  3840 × 2160
FPS         60
Bitrate     18.2 Mbps

Decoder
────────────────────────
Hardware    Yes
Decoder     <device decoder>

Renderer
────────────────────────
GPU         <device GPU>
```

---

# 9. AV1 Requirements

AV1 is a high-priority codec.

The following should be part of the test matrix:

- AV1 8-bit
- AV1 10-bit
- 1080p AV1
- 4K AV1
- AV1 60 FPS
- High-bitrate AV1
- AV1 with multiple audio tracks
- AV1 with subtitles

Expected behavior:

```text
Hardware AV1 available
        ↓
Use hardware decoding

Hardware AV1 unavailable
        ↓
Use software decoding where possible
        ↓
Expose decoder information
```

The application should not falsely report hardware decoding when software decoding is being used.

---

# 10. Player Screen

The player should provide a clean, unobtrusive interface.

Example:

```text
┌──────────────────────────────────┐
│                         ⋮        │
│                                  │
│                                  │
│                                  │
│             VIDEO                │
│                                  │
│                                  │
│                                  │
│                                  │
│                                  │
│  00:32:15 ───────────── 01:42:30 │
│                                  │
│      ◀      ▶      ▶▶            │
│                                  │
└──────────────────────────────────┘
```

Controls should automatically hide after a configurable period.

---

# 11. Gesture Controls

Gestures are a major UX requirement.

## 11.1 Horizontal Swipe

Seek backward/forward.

```text
←                         →
Seek backward          Seek forward
```

## 11.2 Left Vertical Swipe

Control brightness.

```text
Swipe ↑ → Increase brightness
Swipe ↓ → Decrease brightness
```

## 11.3 Right Vertical Swipe

Control volume.

```text
Swipe ↑ → Increase volume
Swipe ↓ → Decrease volume
```

## 11.4 Double Tap

```text
Left       → Seek backward
Center     → Play/Pause
Right      → Seek forward
```

Default seek interval:

```text
10 seconds
```

This should be configurable later.

## 11.5 Pinch

```text
Pinch in  → Fit
Pinch out → Zoom
```

## 11.6 Long Press

Optional:

```text
Long press → Temporary 2x playback speed
```

---

# 12. Subtitle Support

Initial requirements:

- SRT
- ASS/SSA
- VTT
- Embedded subtitles
- External subtitle files
- Subtitle selection
- Subtitle enable/disable
- Subtitle size
- Subtitle position
- Subtitle styling
- Subtitle delay

Future:

- Subtitle downloading
- Subtitle translation
- OCR subtitles

These should not be part of the initial MVP.

---

# 13. Audio Support

Initial requirements:

- Embedded audio tracks
- Audio track selection
- Volume control
- Mute
- Audio delay
- Stereo/mono controls where appropriate

Future:

- Audio passthrough
- Equalizer
- Advanced audio filters
- DRC configuration

---

# 14. Video Controls

Required controls:

- Play/Pause
- Seek
- Playback speed
- Aspect ratio
- Zoom
- Crop
- Rotate
- Fullscreen
- Screen lock
- Subtitle controls
- Audio track selection

Initial playback speeds:

```text
0.25x
0.5x
0.75x
1.0x
1.25x
1.5x
1.75x
2.0x
```

Custom playback speed can be added later.

---

# 15. File Browser

The application should support browsing local storage.

Target sources:

```text
Internal Storage
SD Card
USB Storage
```

Initial features:

- Folder browsing
- Video filtering
- Grid view
- List view
- Sort by name
- Sort by date
- Sort by size
- Search
- Recently played

Avoid building a complete file manager.

Use Android storage APIs wherever possible.

---

# 16. Media Library

Home screen should provide:

```text
┌───────────────────────────────┐
│ MPlayerX                  🔍  │
├───────────────────────────────┤
│ Continue Watching              │
│                               │
│ [video] [video] [video]       │
│                               │
├───────────────────────────────┤
│ Folders                        │
│                               │
│ Movies                         │
│ TV Shows                       │
│ Downloads                      │
│                               │
├───────────────────────────────┤
│ Recently Played                │
│                               │
│ [video] [video] [video]       │
└───────────────────────────────┘
```

---

# 17. Resume Playback

Store the last playback position for each video.

Example:

```text
Movie.mkv

Last position:
01:23:14
```

When opened again:

```text
Resume from 01:23:14?

[ Resume ]    [ Start Over ]
```

Settings:

```text
Always resume
Ask every time
Always start from beginning
```

---

# 18. Picture-in-Picture

Initial PiP support:

- Android Picture-in-Picture
- Continue video playback in PiP
- Play/pause controls
- Previous/next controls where applicable

---

# 19. Background Playback

Initial requirements:

- Audio playback with screen off
- Media notification
- Lock-screen media controls
- Bluetooth/headset media controls

Video background playback can be considered separately.

---

# 20. Settings

Initial settings structure:

```text
Settings

Playback
├── Resume playback
├── Default playback speed
├── Default aspect ratio
├── Hardware acceleration
├── Auto rotate
└── Background playback

Subtitles
├── Size
├── Style
├── Position
└── Delay

Gestures
├── Seek
├── Brightness
├── Volume
└── Double tap

Performance
├── Hardware decoder
├── Renderer
└── Buffer configuration

Interface
├── Theme
├── Orientation
└── Player controls
```

---

# 21. Network Playback

**Not included in V1.**

Potential future support:

```text
HTTP
HTTPS
SMB
FTP
SFTP
WebDAV
DLNA
```

The first release should focus on being an excellent local video player.

---

# 22. Android TV

Android TV is **not part of V1**.

The architecture should avoid making future TV support impossible, but the initial implementation should be optimized for phones/tablets.

---

# 23. Architecture Principles

The project should follow these principles:

### Native First

Use Android platform APIs wherever practical.

### Playback Isolation

The UI must not know about libmpv internals.

### No Blocking Operations

File scanning, database operations and media analysis must never block the main thread.

### Minimal Dependencies

Avoid adding libraries for functionality that Android already provides.

### Performance First

Avoid premature abstraction, unnecessary allocations and excessive recomposition.

### Offline First

Core playback must work without an internet connection.

### Testable

Playback logic, media scanning and persistence should be independently testable.

---

# 24. Development Milestones

## Milestone 1 — Android Shell

Implement:

- Kotlin project
- Jetpack Compose
- Navigation
- Theme
- Settings
- Basic application structure

---

## Milestone 2 — libmpv Integration

Implement:

- NDK setup
- JNI bridge
- libmpv integration
- Video surface
- Open video
- Play
- Pause
- Seek
- Stop
- Release

**Goal:** Play the first local video successfully.

---

## Milestone 3 — Player UX

Implement:

- Playback controls
- Gesture controls
- Double-tap seeking
- Brightness gesture
- Volume gesture
- Pinch-to-zoom
- Fullscreen
- Screen lock
- Playback speed
- Aspect ratio

---

## Milestone 4 — Media Library

Implement:

- MediaStore integration
- Folder browser
- Video discovery
- Thumbnails
- Search
- Sorting
- Playback history
- Resume position

---

## Milestone 5 — Advanced Playback

Implement:

- Audio track selection
- Subtitle selection
- Subtitle delay
- Subtitle styling
- Decoder information
- Playback statistics
- Video information

---

## Milestone 6 — Android Integration

Implement:

- Picture-in-Picture
- Media notification
- Lock-screen controls
- Bluetooth controls
- Screen rotation
- Background audio playback

---

## Milestone 7 — Performance & Device Testing

Test across:

- Google Pixel
- Samsung
- OnePlus
- Xiaomi
- Motorola
- Snapdragon devices
- MediaTek devices

Test:

```text
H.264
HEVC
AV1
4K
60 FPS
10-bit
HDR
High bitrate
Large MKV files
Multiple audio tracks
Multiple subtitle tracks
```

---

# 25. MVP Scope

The first usable release should contain only:

```text
✓ Open local video
✓ libmpv playback
✓ Hardware acceleration
✓ Play/Pause
✓ Seek
✓ Volume
✓ Brightness
✓ Playback speed
✓ Aspect ratio
✓ Zoom
✓ Fullscreen
✓ Audio tracks
✓ Subtitles
✓ Subtitle delay
✓ Resume playback
✓ Basic file browser
✓ Playback history
✓ Dark theme
```

Do **not** initially implement:

```text
✗ Cloud sync
✗ User accounts
✗ Online subtitle search
✗ Streaming services
✗ SMB
✗ Chromecast
✗ DLNA
✗ Equalizer
✗ Video downloading
✗ AI features
```

These can be evaluated after the core player is stable.

---

# 26. Performance Acceptance Criteria

The MVP should meet the following targets on a modern Android device:

| Metric | Target |
|---|---|
| App launch | <500 ms |
| Local video startup | <1 sec where practical |
| UI frame rate | ≥60 FPS |
| Gesture latency | Near-instant / no perceptible lag |
| Main-thread blocking | None for I/O |
| Large file handling | No full-file memory loading |
| Hardware decoding | Preferred when available |
| AV1 | Hardware decoding where device supports it |
| Offline playback | Fully supported |
| Crash-free playback | Required for supported test media |

These are targets rather than universal guarantees because Android hardware and OEM implementations vary significantly.

---

# 27. AI-Assisted Development Rules

AI will be used heavily during development.

However, the project should be developed incrementally.

Do **not** ask AI:

> Build the entire video player.

Instead, implement one milestone/feature at a time.

Example:

```text
Implement Milestone 2.

Do not modify the UI.

Create:
- PlayerEngine
- MpvPlayerEngine
- JNI bridge
- libmpv initialization

Follow the architecture defined in REQUIREMENTS.md.

Before writing code:
1. Inspect the existing project.
2. Identify files that need modification.
3. Explain the implementation plan.

Then implement the changes.
```

For individual features:

```text
Implement double-tap seeking.

Requirements:
- Left side: seek backward
- Center: play/pause
- Right side: seek forward
- Default seek interval: 10 seconds

Do not modify the libmpv integration.
Do not introduce new dependencies.
Preserve the existing architecture.
```

---

# 28. Recommended AI Development Workflow

Maintain these project files:

```text
/
├── REQUIREMENTS.md
├── ARCHITECTURE.md
├── DEVELOPMENT.md
├── TODO.md
└── app/
```

### REQUIREMENTS.md

Product requirements and acceptance criteria.

### ARCHITECTURE.md

Technical architecture and design decisions.

### DEVELOPMENT.md

Build instructions and development environment.

### TODO.md

Current implementation tasks.

This gives the AI a persistent source of truth and reduces the chance of it rewriting architecture every time a new feature is requested.

---

# 29. First Development Goal

The first milestone should be extremely small:

```text
Android App
    ↓
Select local video
    ↓
Open player
    ↓
libmpv
    ↓
Hardware decode
    ↓
Video displayed
    ↓
Play / Pause / Seek
```

Do not build the media library or fancy UI before this works.

Once the playback pipeline is stable, build the MX Player-style experience around it.

---

# 30. Final Technology Decision

For an **Android-only, high-performance video player**, the recommended architecture is:

```text
                    ┌─────────────────┐
                    │ Jetpack Compose │
                    │      UI         │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ PlayerController│
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │  PlayerEngine   │
                    └────────┬────────┘
                             │
                             ▼
                    ┌─────────────────┐
                    │ MpvPlayerEngine │
                    └────────┬────────┘
                             │
                             ▼
                         JNI / NDK
                             │
                             ▼
                         libmpv
                             │
                  ┌──────────┴──────────┐
                  ▼                     ▼
             HW Decoder              GPU
                  │                     │
                  └──────────┬──────────┘
                             ▼
                          Display
```

**Core stack:**

> **Kotlin + Jetpack Compose + Android NDK/JNI + libmpv**

This keeps the UI modern and maintainable while keeping the critical video path native and performance-oriented.