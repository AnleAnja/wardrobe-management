# Changelog

All notable changes to Wardrobe are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- In-app light / dark / system theme setting
- Optional background removal for wardrobe photos

## [1.1.0] - 2026-10-08

### Added
- Backups as ZIP files that include all photos, so a restore brings back items, outfits, calendar entries and images ([#17](https://github.com/AnleAnja/wardrobe-management/issues/17))
- Choice between **Merge** (add to the current wardrobe) and **Replace all** when importing a backup ([#17](https://github.com/AnleAnja/wardrobe-management/issues/17))
- Merging the same backup again, including one from another phone, updates the existing entries instead of creating duplicates
- Backups record a format version and the installation they came from; older v1.0 JSON backups can still be imported ([#23](https://github.com/AnleAnja/wardrobe-management/issues/23))
- Clear error messages for corrupt, unsupported or oversized backups and for low storage
- Automated build and unit tests on GitHub Actions ([#20](https://github.com/AnleAnja/wardrobe-management/issues/20))
- Database migration tests against the exported v1.0 schema ([#24](https://github.com/AnleAnja/wardrobe-management/issues/24))

### Changed
- New photos are resized to at most 1600 px and compressed, which keeps the app's storage use small. Photos with transparent areas stay PNG, so cut-outs still layer on the outfit canvas ([#25](https://github.com/AnleAnja/wardrobe-management/issues/25))
- The outfit image is now drawn from the item photos when you save, instead of being captured from the screen

### Fixed
- Export no longer reports success when the destination file could not be written, and it no longer leaves an empty file behind ([#21](https://github.com/AnleAnja/wardrobe-management/issues/21))
- The outfit canvas image is saved reliably on the first save ([#30](https://github.com/AnleAnja/wardrobe-management/issues/30))
- The outfit canvas keeps item positions, sizes and order when the device is rotated ([#30](https://github.com/AnleAnja/wardrobe-management/issues/30))
- Photos that can't be read from a backup no longer replace a working photo on the device

### Notes
- Update by installing the new APK over v1.0.0. Do **not** uninstall first, or your data is lost. Your wardrobe and photos are kept, and the database is upgraded automatically.
- After updating, create a new backup: unlike v1.0 JSON exports, it contains your photos.

## [1.0.0] - 2026-07-23

First public release on GitHub. Free sideload APK — no account required.

### Added
- Wardrobe catalog with photos, categories, subcategories, seasons, ratings, and wear tracking
- Outfit creation with single outfit photo and drag-and-resize canvas editor
- Calendar planning with scheduled outfits and optional temperature
- JSON import/export for backup and restore (metadata)
- Filter and sort for wardrobe items and outfits
- Inspiration tab with embedded web content
- About screen with version info and privacy policy link
- Unit tests for filter/sort, JSON parsing, categories, and UI helpers

### Changed
- Package migration to `com.anleanja.wardrobe`
- Navigation refactor with split UI files and simplified outfit images (letterboxing for items, original aspect ratio for outfits)
- Release build setup with optional signing via `keystore.properties`

### Fixed
- Outfit planner bugs for sorting, wear stats, and UI interactions
- Item detail screen no longer loads indefinitely when an item is missing

### Notes
- **Android 8.0+** (API 26) required
- JSON import restores text data; **photos must be re-assigned** after migrating from older builds (`com.example.wardrobe`) because export stores file paths, not image files
- Privacy policy: https://anleanja.github.io/wardrobe-management/privacy.html

[Unreleased]: https://github.com/AnleAnja/wardrobe-management/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/AnleAnja/wardrobe-management/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/AnleAnja/wardrobe-management/releases/tag/v1.0.0
