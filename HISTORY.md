# HISTORY.md — MPlayerX prompt / decision log

Newest first. One entry per user request: ask → decision → commit.

## 2026-09-16 — Agent memory + history log

- Asked: add AGENTS.md so all prompts are saved as history.
- Did: `AGENTS.md` (commands, rules, mandatory history logging), `HISTORY.md` (this file, backfilled).
- Commit: (this commit).

## 2026-09-16 — GitHub Releases with APK + notes

- Asked: create releases with APK output + release notes.
- Did: `v0.1.0-mvp` release with `app-debug.apk` + notes; `.github/workflows/release.yml`
  (tag `v*` → CI builds debug APK, attaches to release).
- Skipped: Play Store signing (add when store distribution is real).
- Commit: `62c97b6`.

## 2026-09-16 — Enrich repo docs

- Asked: no docs/readme present, enrich repo; commit+push as I go.
- Did: `README.md` (features, gestures, stack, build, layout); fixed stale Gradle path in `DEVELOPMENT.md`.
- Commit: `621d49a`.

## 2026-09-16 — Confirm commits

- Asked: did you commit the code changes?
- Did: verified `main` clean, in sync with `origin/main` (2 commits at the time).

## 2026-09-16 — Wire repo, ponytail mode, continue build

- Asked: use `QBT-VinayAdiga/adiga-media-player` for this dir, push per update; ponytail full; continue.
- Did: repo was empty → `git init -b main`, `.gitignore`, initial push.
- Build fixes: Media3 `1.5.1`→`1.4.1` (1.5.x needs compileSdk 35; avoids SDK download),
  `TrackGroup.indexOf` lambda → index loop. `BUILD SUCCESSFUL`, 22 MB APK.
- Commits: `866576c` (MVP), `b7a5740` (build fixes).

## 2026-09-16 — Initial implementation to local APK

- Asked: implement `Android Video Player — Initial Requirements.md` to a local testable APK.
- Decisions: Media3/ExoPlayer behind `PlayerEngine` instead of libmpv for first APK
  (libmpv `.so`+JNI is Milestone-2 sized; `MpvPlayerEngine` stub holds the swap);
  manual DI; Room + DataStore + Coil; minSdk 26, compileSdk 34.
- Did: full MVP (home/browser/player/settings, all §11 gestures, resume, PiP, background audio);
  installed Android SDK `D:\android-sdk` + Gradle 8.10.2; `ARCHITECTURE.md`, `DEVELOPMENT.md`, `TODO.md`.
