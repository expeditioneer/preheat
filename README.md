# Preheat

Send SMS commands to your auxiliary heater straight from your Android phone.

[![License: GPL v2](https://img.shields.io/badge/License-GPL%20v2-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/min%20SDK-31-brightgreen.svg)](https://apilevels.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-7F52FF.svg?logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-2026.05-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)

<p align="center">
  <img src="docs/screenshots/heater.png" alt="Heater screen" width="30%"/>
  <img src="docs/screenshots/config.png" alt="Config screen" width="30%"/>
  <img src="docs/screenshots/gps.png" alt="GPS-Tracker screen" width="30%"/>
</p>

## Features

- Manual heater commands over SMS: Start, Stop, Summer, Winter, Temperature request, Status request
- GPS position request for compatible GPS trackers (Waypoint `$WP+GETLOCATION` protocol)
- Per-command text fully configurable — works with any heater that accepts text-message commands
- Optional SMS counter with monthly limit and prepaid-credit tracking
- Localized in English, German, Spanish, French, and Italian
- Material You dynamic color, follows system light/dark theme
- Edge-to-edge, single-Activity Compose UI

## Requirements

- Android 12 (API 31) or newer
- A SIM that can send SMS to the heater's phone number
- The heater (or its GSM module) accepting plain text commands

## Building

### With Nix (recommended)

The repo ships a `flake.nix` that pins the Android SDK, JDK 17, and the Android command-line tools.

```sh
nix develop
./gradlew assembleDebug
```

The devshell sets `ANDROID_USER_HOME` and `GRADLE_USER_HOME` inside the repo, so it leaves no global Android state behind.

### Without Nix

Provide JDK 17 and an Android SDK with platform 36 and build-tools 36.0.0, then:

```sh
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.

## Tech stack

| Layer        | Choice |
| ------------ | ------ |
| Language     | Kotlin 2.3.21 |
| UI           | Jetpack Compose (BOM 2026.05.01), Material 3 |
| Architecture | Single Activity + per-screen ViewModel |
| Persistence  | androidx.datastore (Preferences) |
| Build        | AGP 9.2.1 / Gradle (Kotlin DSL) |
| Min / Target | Android 12 (API 31) / Android 16 (API 36) |

## License

GPL-2.0 — see [LICENSE](LICENSE).
