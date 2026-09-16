# DEVELOPMENT.md — MPlayerX

## Prereqs
- JDK 17+ (`java -version`)
- Android SDK at `D:\android-sdk` (platforms;android-34, build-tools;34.0.0)
  or set `sdk.dir` in `local.properties`
- Gradle 8.10+ (`D:\gradle-8.10.2\bin\gradle.bat` on this machine)

## Build debug APK
```
D:\gradle-8.10.2\bin\gradle.bat :app:assembleDebug
REM → app\build\outputs\apk\debug\app-debug.apk
```

## Install
```
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Open a video from PC for testing
```
adb push sample.mp4 /sdcard/Movies/
```

## Notes
- First build downloads AGP/Kotlin/Media3 deps (~500 MB, one time).
- `local.properties` points at `D:\android-sdk`; adjust on other machines.
