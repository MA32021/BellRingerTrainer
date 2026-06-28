# Извлечь все .so и проверить каждое
TMPDIR=$(mktemp -d)
unzip -q app/build/outputs/apk/debug/app-debug.apk "lib/arm64-v8a/*.so" -d "$TMPDIR"

NDK=$(ls -d "$ANDROID_HOME/ndk/"* 2>/dev/null | sort -V | tail -1)
READELF="$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-readelf"

echo "=== Проверка выравнивания всех .so ==="
for so in "$TMPDIR"/lib/arm64-v8a/*.so; do
    name=$(basename "$so")
    align=$("$READELF" -l "$so" 2>/dev/null | grep LOAD | awk '{print $NF}' | sort -u | tr '\n' ' ')
    if echo "$align" | grep -q "0x1000"; then
        echo "❌ $name  Align: $align"
    else
        echo "✅ $name  Align: $align"
    fi
done

rm -rf "$TMPDIR"

