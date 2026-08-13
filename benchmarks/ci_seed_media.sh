#!/bin/bash
#
# Seeds a CI emulator with images and videos so the scroll benchmarks
# have real MediaStore content to scroll through.
#
# Usage: ./benchmarks/ci_seed_media.sh [image_count] [video_count]
#

set -euo pipefail

IMAGE_COUNT="${1:-40}"
VIDEO_COUNT="${2:-6}"

adb wait-for-device

adb shell mkdir -p /sdcard/Pictures /sdcard/Movies

echo "Seeding $IMAGE_COUNT images..."
for i in $(seq 1 "$IMAGE_COUNT"); do
    adb shell screencap -p "/sdcard/Pictures/seed_img_$i.png"
done

echo "Seeding $VIDEO_COUNT videos..."
for i in $(seq 1 "$VIDEO_COUNT"); do
    adb shell screenrecord --time-limit 2 --size 480x854 "/sdcard/Movies/seed_vid_$i.mp4" || true
done

# Trigger a MediaStore scan so the app's MediaStore queries see the files (API 30+).
adb shell content call --uri content://media/none --method scan_volume --arg external_primary >/dev/null 2>&1 || true

# Fallback: per-file scan broadcast for images older scanners pick up.
for f in $(adb shell ls /sdcard/Pictures 2>/dev/null | tr -d '\r'); do
    adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Pictures/$f" >/dev/null 2>&1 || true
done
for f in $(adb shell ls /sdcard/Movies 2>/dev/null | tr -d '\r'); do
    adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d "file:///sdcard/Movies/$f" >/dev/null 2>&1 || true
done

echo "Seeded: $(adb shell ls /sdcard/Pictures | wc -l | tr -d ' ') images, $(adb shell ls /sdcard/Movies | wc -l | tr -d ' ') videos"
