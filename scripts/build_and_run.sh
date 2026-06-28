#!/bin/bash
set -e

# ✅ Приоритет: хардкодный TCL → автоопределение → эмулятор
TCL_SERIAL="3CEF422D1D67607"

if adb devices 2>/dev/null | grep -q "$TCL_SERIAL"; then
    FLAVOR="Real"
    FLAVOR_LOWER="real"
    TARGET_DEVICE="$TCL_SERIAL"
    echo "📱 TCL detected ($TARGET_DEVICE) → building realDebug"
else
    # Фолбэк: автоопределение (|| true предотвращает выход по set -e)
    REAL_DEVICES=$(adb devices -l 2>/dev/null | grep -v "List of devices" | grep -v "^$" | grep -viE "emulator|sdk_gphone|ranchu" || true)
    REAL_COUNT=0
    if [ -n "$REAL_DEVICES" ]; then
        REAL_COUNT=$(echo "$REAL_DEVICES" | wc -l | tr -d ' ')
    fi

    if [ "$REAL_COUNT" -gt 0 ]; then
        FLAVOR="Real"
        FLAVOR_LOWER="real"
        TARGET_DEVICE=$(echo "$REAL_DEVICES" | head -1 | awk '{print $1}')
        echo "📱 Real device detected ($TARGET_DEVICE) → building realDebug"
    else
        FLAVOR="Emulator"
        FLAVOR_LOWER="emulator"
        TARGET_DEVICE=""
        echo "📱 Emulator detected → building emulatorDebug"
    fi
fi

ADB_TARGET=""
if [ -n "$TARGET_DEVICE" ]; then
    ADB_TARGET="-s $TARGET_DEVICE"
fi

echo "🔨 Building..."
./gradlew "assemble${FLAVOR}Debug"

APK="app/build/outputs/apk/${FLAVOR_LOWER}/debug/app-${FLAVOR_LOWER}-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ APK not found: $APK"
    ls -la app/build/outputs/apk/ 2>/dev/null || echo "No apk output directory"
    exit 1
fi

# Патчим только real-сборку
# Патчим только real-сборку
if [ "$FLAVOR" = "Real" ]; then
    echo "🔧 Patching 16KB alignment..."
    ./scripts/patch_16kb.sh "$APK" || {
        echo "⚠️ Patch script returned non-zero, but continuing..."
    }
else
    echo "⏭️ Skipping 16KB patch (emulator build has no MediaPipe)"
fi


echo "📱 Installing on ${TARGET_DEVICE:-emulator}..."
adb $ADB_TARGET install -r "$APK"

echo "🚀 Launching..."
adb $ADB_TARGET shell am start -n com.bellringer.trainer/.MainActivity

echo "📋 Tailing logs (Ctrl+C to stop)..."
adb $ADB_TARGET logcat -c
adb $ADB_TARGET logcat -s "AndroidRuntime:E" "BellAudioEngine:D" "HandLandmarkerHelper:D" "CameraInit:D" "NoOpLandmarker:D" "DEBUG_TRAINING:D" "KalmanFilter:D" "LandmarkSmoother:D"

