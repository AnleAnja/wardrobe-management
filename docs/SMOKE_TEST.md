# Smoke test checklist

Run this checklist before attaching an APK to a GitHub Release.
Test the **signed release APK** (`assembleRelease`), not a debug build.

**Device:** real phone preferred (sideload matches how users install)  
**Time:** ~15–20 minutes

---

## Install

- [ ] Uninstall old dev build (`com.example.wardrobe`) if still present
- [ ] Install `app-release.apk` (via file manager or `adb install -r`)
- [ ] App opens without crash on cold start

---

## Navigation

- [ ] All tabs open: Wardrobe, Outfits, Calendar, Inspiration, About
- [ ] Back navigation and bottom nav work without crash
- [ ] Rotate screen once on a detail screen — no crash

---

## Wardrobe

- [ ] Add item with photo
- [ ] Photo shows with letterboxing (full image visible, not cropped to 1:1)
- [ ] Save category, season, rating
- [ ] Open item detail from list
- [ ] Change filter or sort — list updates
- [ ] Edit item
- [ ] Delete item

---

## Outfits

- [ ] Add outfit with single photo (original aspect ratio)
- [ ] Optional: arrange items in canvas editor and save
- [ ] Open outfit detail
- [ ] Edit or delete outfit

---

## Calendar

- [ ] Schedule an outfit for a date
- [ ] Open scheduled entry
- [ ] Remove or change schedule

---

## Backup (JSON)

- [ ] Export JSON to a file
- [ ] Import the same JSON (or a test backup)
- [ ] Wardrobe/outfit **metadata** is restored
- [ ] Note: photos may be missing after import if paths pointed to another install — known limitation in v1.0.0

---

## About & links

- [ ] About shows correct version (e.g. **1.0.0 (1)**)
- [ ] Privacy policy link opens in browser

---

## Inspiration (optional, needs internet)

- [ ] Inspiration tab loads web content

---

## Stability

- [ ] Kill app from recents, reopen — data still present
- [ ] Background app briefly, return — no crash

---

## Release sign-off

| Field | Value |
|-------|--------|
| APK tested | `app-release.apk` |
| Version | |
| Device / Android | |
| Tester | |
| Date | |
| Result | Pass / Fail |

If **Fail**, do not publish the release — open a bug issue with steps and Android version.
