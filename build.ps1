# RedMagic Fix 构建脚本（Windows PowerShell）
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$bt = "$sdk\build-tools\35.0.0"
$androidJar = "$sdk\platforms\android-35\android.jar"

$out = "build"
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force "$out\classes", "$out\apk" | Out-Null

Write-Host "== 1/5 javac =="
$srcs = Get-ChildItem app/src/main/java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -source 8 -target 8 -cp $androidJar -d "$out\classes" @srcs

Write-Host "== 2/5 删除 Xposed stub =="
Remove-Item -Recurse -Force "$out\classes\de"

Write-Host "== 3/5 d8 =="
$classes = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& java -cp "$bt\lib\d8.jar" com.android.tools.r8.D8 --lib $androidJar --release --min-api 29 --output "$out\apk" @classes

Write-Host "== 4/5 aapt2 =="
& "$bt\aapt2.exe" compile --dir app/src/main/res -o "$out\res.zip"
& "$bt\aapt2.exe" link -o "$out\base.apk" -I $androidJar --manifest app/src/main/AndroidManifest.xml -A app/src/main/assets --min-sdk-version 29 "$out\res.zip"

Write-Host "== 5/5 合并 dex + 签名 =="
Add-Type -AssemblyName System.IO.Compression.FileSystem
Copy-Item "$out\base.apk" "$out\unsigned.apk" -Force
$zip = [System.IO.Compression.ZipFile]::Open("$out\unsigned.apk", "Update")
try {
    $entry = $zip.CreateEntry("classes.dex", [System.IO.Compression.CompressionLevel]::Optimal)
    $es = $entry.Open()
    $ds = [System.IO.File]::OpenRead("$out\apk\classes.dex")
    $ds.CopyTo($es)
    $ds.Close(); $es.Close()
} finally { $zip.Dispose() }

$keystore = "redmagicfix.keystore"
if (-not (Test-Path $keystore)) {
    $javaBin = (Get-Command java).Source | Split-Path
    $keytool = Join-Path $javaBin "keytool.exe"
    if (-not (Test-Path $keytool)) {
        $keytool = "C:\Program Files\Java\jdk-17\bin\keytool.exe"
    }
    & $keytool -genkeypair -v -keystore $keystore -storepass android -alias redmagicfix -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=RedMagicFix, OU=RedMagic, O=RedMagic, C=CN"
}
& java -jar "$bt\lib\apksigner.jar" sign --ks $keystore --ks-pass pass:android --ks-key-alias redmagicfix --out "$out\RedMagicFix-v1.5.apk" "$out\unsigned.apk"
& java -jar "$bt\lib\apksigner.jar" verify "$out\RedMagicFix-v1.5.apk"
Get-Item "$out\RedMagicFix-v1.5.apk" | Select-Object FullName, Length
