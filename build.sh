#!/usr/bin/env bash
# SplitFix Xposed 模块构建脚本 (git-bash / Windows)
# 链: javac -> (删stub) -> d8 -> aapt2 link -> 加 dex -> apksigner
set -e
cd "$(dirname "$0")"

SDK="$LOCALAPPDATA/Android/Sdk"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-35"
JAVA_HOME_DIR=$(java -XshowSettings:properties -version 2>&1 | grep 'java.home' | sed 's/.*= //')
KEYTOOL="$JAVA_HOME_DIR/bin/keytool.exe"
KEYSTORE="redmagicfix.keystore"
PKG="com.redmagic.fix"

SRC="app/src/main/java"
OUT="build"
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/apk"

echo "== 1/5 javac =="
find "$SRC" -name "*.java" > "$OUT/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$PLATFORM/android.jar" \
  -d "$OUT/classes" @"$OUT/sources.txt" 2>&1 | head -20

echo "== 2/5 删除 Xposed stub 类 (绝不能进 dex) =="
rm -rf "$OUT/classes/de"
find "$OUT/classes" -name "*.class" | head -5

echo "== 3/5 d8 -> dex =="
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --lib "$PLATFORM/android.jar" \
  --release --min-api 29 \
  --output "$OUT/apk" \
  $(find "$OUT/classes" -name "*.class")
ls -la "$OUT/apk/"

echo "== 4/5 aapt2 编译资源 + link (含 assets/xposed_init) =="
"$BT/aapt2.exe" compile --dir app/src/main/res -o "$OUT/res.zip"
"$BT/aapt2.exe" link -o "$OUT/base.apk" \
  -I "$PLATFORM/android.jar" \
  --manifest app/src/main/AndroidManifest.xml \
  -A app/src/main/assets \
  --min-sdk-version 29 \
  "$OUT/res.zip"

echo "== 5/5 合并 dex + 签名 =="
python - "$OUT/base.apk" "$OUT/apk/classes.dex" "$OUT/unsigned.apk" <<'PYEOF'
import sys, zipfile, shutil
base, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
shutil.copy(base, out)
with zipfile.ZipFile(out, 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(dex, 'classes.dex')
print("dex added")
PYEOF

if [ ! -f "$KEYSTORE" ]; then
  "$KEYTOOL" -genkeypair -v -keystore "$KEYSTORE" \
    -storepass android -alias splitfix -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=SplitFix, OU=Hermes, O=Hermes, C=CN" >/dev/null 2>&1
fi
java -jar "$BT/lib/apksigner.jar" sign \
  --ks "$KEYSTORE" --ks-pass pass:android --ks-key-alias splitfix \
  --out "$OUT/RedMagicFix-v1.4.apk" "$OUT/unsigned.apk"
java -jar "$BT/lib/apksigner.jar" verify "$OUT/RedMagicFix-v1.4.apk" && echo "SIGN OK"
ls -la "$OUT/RedMagicFix-v1.4.apk"
