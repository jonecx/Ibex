#!/bin/bash
#
# Seeds a CI emulator with images and videos so the scroll benchmarks
# have real MediaStore content to scroll through.
#
# Usage: ./benchmarks/ci_seed_media.sh [image_count] [video_count]
#

set -euo pipefail

# Counts must overflow the grid viewport: a 4-column grid fits ~26 tiles per
# screen, and a grid that fits on one screen reports itself as not scrollable.
IMAGE_COUNT="${1:-40}"
VIDEO_COUNT="${2:-48}"

adb wait-for-device

# Boot-completed does not imply /sdcard is mounted yet, so probe until it is writable.
for i in $(seq 1 30); do
    if adb shell "touch /sdcard/.seed_probe && rm /sdcard/.seed_probe" >/dev/null 2>&1; then
        break
    fi
    [ "$i" = 30 ] && { echo "Error: /sdcard never became writable"; exit 1; }
    sleep 2
done

adb shell mkdir -p /sdcard/Pictures /sdcard/Movies

echo "Seeding $IMAGE_COUNT images..."
for i in $(seq 1 "$IMAGE_COUNT"); do
    adb shell screencap -p "/sdcard/Pictures/seed_img_$i.png"
done

# screenrecord is unreliable on headless emulators, so push real sample MP4s from the test assets.
echo "Seeding $VIDEO_COUNT videos..."
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ASSETS_DIR="$SCRIPT_DIR/../app/src/androidTest/assets"
SAMPLES=("$ASSETS_DIR/earth_MP4_480_1_5MG.mp4" "$ASSETS_DIR/15764815-hd_1080_1920_60fps.mp4")
for i in $(seq 1 "$VIDEO_COUNT"); do
    src="${SAMPLES[$((i % 2))]}"
    adb push "$src" "/sdcard/Movies/seed_vid_$i.mp4" >/dev/null
done

# MediaStore does not reliably auto-index adb-pushed files, so scan each video explicitly (API 30+).
for f in $(adb shell ls /sdcard/Movies 2>/dev/null | tr -d '\r'); do
    adb shell content call --uri content://media/none --method scan_file --arg "/sdcard/Movies/$f" >/dev/null 2>&1 || true
done
adb shell content call --uri content://media/none --method scan_volume --arg external_primary >/dev/null 2>&1 || true

# Verify MediaStore actually indexed the videos before handing off to the benchmarks.
for i in $(seq 1 15); do
    vids=$(adb shell content query --uri content://media/external/video/media --projection _id 2>/dev/null | grep -c "Row:" || true)
    imgs=$(adb shell content query --uri content://media/external/images/media --projection _id 2>/dev/null | grep -c "Row:" || true)
    echo "MediaStore sees: $imgs images, $vids videos (check $i)"
    [ "$vids" -ge "$VIDEO_COUNT" ] && break
    adb shell content call --uri content://media/none --method scan_volume --arg external_primary >/dev/null 2>&1 || true
    sleep 4
done

echo "Seeded: $(adb shell ls /sdcard/Pictures | wc -l | tr -d ' ') images, $(adb shell ls /sdcard/Movies | wc -l | tr -d ' ') videos on disk"
