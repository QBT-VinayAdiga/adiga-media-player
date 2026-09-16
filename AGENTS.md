# AGENTS.md — MPlayerX

## Commands

```powershell
D:\gradle-8.10.2\bin\gradle.bat :app:assembleDebug   # APK → app\build\outputs\apk\debug\
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

SDK: `D:\android-sdk` (see `local.properties`, git-ignored). JDK 17+.

## Rules

- UI must depend only on `playback.PlayerEngine`, never on engine internals.
- libmpv swap stays one line in `MPlayerXApp` — keep `MpvPlayerEngine` stub compiling.
- Ponytail full: YAGNI, stdlib/platform first, smallest diff, no new deps
  without justification. Mark real-corner cuts with `ponytail:` comments.
- No network/TV/equalizer features (V1 non-goals, requirements §25).

## History (mandatory)

Every user request gets one dated entry in HISTORY.md: what was asked,
what was decided/done, commit hash. Write it in the same commit as the
work (docs-only entry if no code changed). Push every commit to `origin/main`.
Release APKs via `gh release create vX.Y.Z --notes-file <notes> <apk>`.
