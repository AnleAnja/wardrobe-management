# Changelog

All notable changes to Wardrobe are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned
- Import/export with bundled images (ZIP) and merge vs replace-all option
- In-app light / dark / system theme setting
- Optional background removal for wardrobe photos

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

[Unreleased]: https://github.com/AnleAnja/wardrobe-management/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/AnleAnja/wardrobe-management/releases/tag/v1.0.0
