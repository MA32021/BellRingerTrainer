#!/bin/bash
set -e

APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"

if [ ! -f "$APK" ]; then
    echo "❌ APK not found: $APK"
    exit 1
fi

SDK_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS=$(ls -d "$SDK_HOME/build-tools/"* 2>/dev/null | sort -V | tail -1)
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"

echo "📦 Patching: $APK"
TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

# 1. Распаковка
unzip -q "$APK" -d "$TMPDIR"

# 2. Патчинг ELF-заголовков
for SO in lib/arm64-v8a/libmediapipe_tasks_vision_jni.so \
          lib/arm64-v8a/libimage_processing_util_jni.so; do
    if [ -f "$TMPDIR/$SO" ]; then
        patchelf --page-size 16384 "$TMPDIR/$SO"
        echo "✅ ELF patched: $SO"
    fi
done

# 3. Переупаковка БЕЗ сжатия
cd "$TMPDIR"
zip -qr0 "$OLDPWD/$APK" .
cd "$OLDPWD"

# 4. ⭐ КЛЮЧЕВОЙ ШАГ: Выравнивание по 16KB внутри APK
"$ZIPALIGN" -f -P 16 4 "$APK" "${APK}.aligned"
mv "${APK}.aligned" "$APK"
echo "✅ ZIP aligned to 16KB pages"

# 5. Переподпись (после zipalign подпись всегда ломается)
"$APKSIGNER" sign \
    --ks "$HOME/.android/debug.keystore" \
    --ks-pass pass:android \
    --key-pass pass:android \
    "$APK"

echo "✅ Done: APK repacked, aligned & re-signed"

SDK_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS=$(ls -d "$SDK_HOME/build-tools/"* 2>/dev/null | sort -V | tail -1)

# Проверка выравнивания 16KB прямо в APK (без извлечения)
"$BUILD_TOOLS/zipalign" -c -P 16384 -v app/build/outputs/apk/debug/app-debug.apk 2>&1 | grep -E "libmediapipe|libimage_processing|Verification"


