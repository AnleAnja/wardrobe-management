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
- ZIP backup import/export, including photos
- Optional inspiration tab (web content)

## Photos and backups

New photos are stored on the device as JPEG. The long edge is limited to 1600 pixels at quality 85, so a typical item photo is a few hundred kilobytes.

Export writes a `.zip` backup with `wardrobe.json` and an `images/` folder. Import can merge that backup into the current wardrobe or replace the wardrobe, including photos. Merging a backup from the same wardrobe updates matching records; a backup from another device is added alongside them, and merging it again updates those copies instead of duplicating them. JSON backups from older versions still import item, outfit, and calendar details. Those files do not contain the photo files.

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
