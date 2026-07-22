# Wardrobe

Android app for cataloging your closet, building outfits, and planning what to wear.

Free to download from [GitHub Releases](https://github.com/AnleAnja/wardrobe-management/releases). No account required — your data stays on your phone.

## Features

- Photo wardrobe catalog with categories, seasons, and wear tracking
- Drag-and-resize outfit builder
- Calendar scheduling with optional temperature
- JSON import/export backup
- Optional inspiration browsing via embedded web content

## Install (pre-built APK)

1. Open [Releases](https://github.com/AnleAnja/wardrobe-management/releases) and download the latest `app-release.apk`.
2. On your phone, allow installation from your browser or files app when prompted (Android calls this “unknown apps” or “install unknown apps”).
3. Open the downloaded APK and tap **Install**.

> **Tip:** If you already had an older dev build installed (`com.example.wardrobe`), uninstall it first before installing the release APK.

## Requirements (build from source)

- Android Studio Ladybug or newer
- JDK 11+
- Android SDK 36

## Build from source

Debug build (local development):

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device or emulator:

```bash
./gradlew installDebug
```

## Release APK (for GitHub Releases)

Release builds are minified and shrunk. Signing is **recommended** so installs and future updates stay consistent.

### 1. Create a release keystore (once)

```bash
keytool -genkey -v -keystore wardrobe-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias wardrobe
```

Keep `wardrobe-release.jks` and your passwords **off Git** (see `.gitignore`).

### 2. Configure signing locally

```bash
cp keystore.properties.example keystore.properties
```

Edit `keystore.properties` with your passwords and keystore path:

```properties
storePassword=your-store-password
keyPassword=your-key-password
keyAlias=wardrobe
storeFile=../wardrobe-release.jks
```

`keystore.properties.example` is a template only — Gradle reads **`keystore.properties`**, which you create locally and never commit.

### 3. Build the release APK

```bash
./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

Attach that file to a GitHub Release. Bump `versionCode`, `versionName`, and `versionTag` in [`launch-config.properties`](launch-config.properties) before each release, then tag matching `versionTag` (e.g. `v1.0.0`).

### Signing without a keystore

If `keystore.properties` is missing, Gradle still builds a release APK, but it may be unsigned or use a debug key depending on your setup. For public GitHub downloads, use a proper release keystore.

## Tests

```bash
./gradlew testDebugUnitTest
```

## Database migrations

v1.0 ships with Room schema version **10** frozen. See [DATABASE.md](DATABASE.md) before changing entities or bumping the database version.

## Launch configuration

Product defaults live in [`launch-config.properties`](launch-config.properties):

| Key | Purpose |
|-----|---------|
| `applicationId` | Android package name (`com.anleanja.wardrobe`) |
| `versionCode` / `versionName` | App version shown in About and used by Android updates |
| `versionTag` | Git tag for GitHub Releases (e.g. `v1.0.0`) |
| `privacyPolicyUrl` | Linked from the in-app About screen |
| `githubRepo` | Project repository URL |

## Privacy & website

- Privacy policy: [privacy.html](website/privacy.html) (host via GitHub Pages from the [`website/`](website/) folder)
- Landing page: see [`website/README.md`](website/README.md)

## Optional: crash reporting

Firebase Crashlytics is **optional** for GitHub distribution. To enable it:

1. Create a Firebase project for package `com.anleanja.wardrobe`.
2. Download `google-services.json` into the `app/` directory (gitignored).
3. Rebuild release builds.

Use [`app/google-services.json.example`](app/google-services.json.example) as a reference. Without Firebase, the app runs normally — crashes are only logged locally.

## License

Add a `LICENSE` file before your first public release (e.g. MIT or Apache 2.0).
