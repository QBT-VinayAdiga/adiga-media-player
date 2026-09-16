# ARCHITECTURE.md — MPlayerX

## Decision
Kotlin + Jetpack Compose + Media3 (ExoPlayer) for MVP, with a `PlayerEngine`
abstraction preserving the requirements' libmpv plan.

## Why not libmpv in the first APK
Building libmpv for Android requires NDK prebuilts (ffmpeg + libmpv `.so` per
ABI) and a JNI render bridge. That is Milestone 2 work. Shipping the first
testable APK on Media3 gives identical MVP behavior (HW decode via MediaCodec,
AV1 where the device supports it, all §7 containers the device demuxes) while
keeping the UI isolated behind `PlayerEngine`.

## libmpv swap (Milestone 2, one-line change)
1. Drop `libmpv.so` per ABI into `app/src/main/jniLibs/`.
2. Implement `native/` JNI bridge; fill in `MpvPlayerEngine` (same interface).
3. `MPlayerXApp`: `ExoPlayerEngine(...)` → `MpvPlayerEngine(...)`. No UI change.

## Modules
- `ui/home|browser|player|settings` — Compose screens, no engine imports
  except `PlayerController`/`PlayerEngine` types.
- `playback/` — `PlayerEngine`, `PlayerState`, `PlayerController`,
  `ExoPlayerEngine`, `MpvPlayerEngine` (stub), `PlaybackService`
  (MediaSession, background audio + notification).
- `media/` — `MediaScanner` (MediaStore, IO dispatcher), `MediaRepository`.
- `database/` — Room `AppDatabase`, `PlaybackHistory` (resume positions).
- `settings/` — DataStore `SettingsRepository`.
- Manual DI in `MPlayerXApp` (allowed by requirements §2).
