# Wardrobe

Catalog your closet, build outfits, and plan what to wear. Free Android app — no account, data stays on your device.

**Download:** [GitHub Releases](https://github.com/AnleAnja/wardrobe-management/releases) → `app-release.apk`

## Install

1. Download the latest `app-release.apk` from [Releases](https://github.com/AnleAnja/wardrobe-management/releases).
2. Open the file on your phone and allow installation when prompted (“unknown apps”).
3. Tap **Install**.

## Features

- Wardrobe catalog with photos, categories, seasons, and wear tracking
- Outfit builder and calendar planning
- JSON import/export backup
- Optional inspiration tab (web content)

## Privacy

[Privacy policy](https://anleanja.github.io/wardrobe-management/privacy.html)

## Bugs & feedback

Found a bug or have a suggestion? [Open an issue](https://github.com/AnleAnja/wardrobe-management/issues/new) on this repository. Include your Android version, app version (About screen), and steps to reproduce if you can.

## Build from source

Requires Android Studio, JDK 11+, SDK 36.

```bash
./gradlew assembleDebug          # dev APK
./gradlew installDebug           # install on connected device
./gradlew testDebugUnitTest      # unit tests
```

**Release APK** (for maintainers): create a keystore, copy `keystore.properties.example` → `keystore.properties`, then `./gradlew assembleRelease`. Output: `app/build/outputs/apk/release/app-release.apk`. Bump version in [`launch-config.properties`](launch-config.properties) before tagging a release.

Developer notes: [DATABASE.md](DATABASE.md) (Room migrations), [`launch-config.properties`](launch-config.properties) (app ID, version, URLs).
See [CHANGELOG.md](CHANGELOG.md) and [docs/SMOKE_TEST.md](docs/SMOKE_TEST.md) before releasing.

## License

See [LICENSE](LICENSE.md).
