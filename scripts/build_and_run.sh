#!/bin/bash
set -e

# ✅ Надёжное определение: считаем реальные устройства vs эмуляторы
REAL_DEVICES=$(adb devices -l | grep -v "List of devices" | grep -v "^$" | grep -viE "emulator|sdk_gphone|ranchu" || true)
REAL_COUNT=$(echo "$REAL_DEVICES" | grep -c "." 2>/dev/null || echo "0")

if [ "$REAL_COUNT" -gt 0 ]; then
    FLAVOR="Real"
    FLAVOR_LOWER="real"
    # Берём серийный номер первого реального устройства
    TARGET_DEVICE=$(echo "$REAL_DEVICES" | head -1 | awk '{print $1}')
    echo "📱 Real device detected ($TARGET_DEVICE) → building realDebug"
else
    FLAVOR="Emulator"
    FLAVOR_LOWER="emulator"
    TARGET_DEVICE=""
    echo "📱 Emulator detected → building emulatorDebug"
fi

echo "🔨 Building..."
./gradlew "assemble${FLAVOR}Debug"

APK="app/build/outputs/apk/${FLAVOR_LOWER}/debug/app-${FLAVOR_LOWER}-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ APK not found: $APK"
    ls -la app/build/outputs/apk/ 2>/dev/null || echo "No apk output directory"
    exit 1
fi

# Патчим только real-сборку (в emulator нет MediaPipe .so)
if [ "$IS_EMULATOR" != "true" ] && [ "$FLAVOR" = "Real" ]; then
    echo "🔧 Patching 16KB alignment..."
    ./scripts/patch_16kb.sh "$APK"
else
    echo "⏭️ Skipping 16KB patch (emulator build has no MediaPipe)"
fi

# ✅ Установка и запуск на КОНКРЕТНОЕ устройство (если найдено)
ADB_TARGET=""
if [ -n "$TARGET_DEVICE" ]; then
    ADB_TARGET="-s $TARGET_DEVICE"
    echo "📱 Targeting device: $TARGET_DEVICE"
fi

TCL_SERIAL="3CEF422D1D67607"

echo "📱 Installing..."
#adb $ADB_TARGET install -r "$APK"
adb -s "$TCL_SERIAL" install -r app/build/outputs/apk/real/debug/app-real-debug.apk

echo "🚀 Launching..."
# adb $ADB_TARGET shell am start -n com.bellringer.trainer/.MainActivity
adb -s "$TCL_SERIAL" shell am start -n com.bellringer.trainer/.MainActivity

echo "📋 Tailing logs (Ctrl+C to stop)..."
adb $ADB_TARGET logcat -c
adb $ADB_TARGET logcat -s AndroidRuntime:E BellAudioEngine:D HandLandmarkerHelper:D CameraInit:D NoOpLandmarker:D DEBUG_TRAINING:D

