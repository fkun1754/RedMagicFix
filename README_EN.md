**English**（[中文](./README.md)）

# SplitFix

> An Xposed module that removes the split-screen ratio limit on Red Magic 8S Pro+ and enables stepless adjustment.

On Red Magic 8S Pro+ (NX729J), the split-screen divider in landscape mode can only snap to 1:1. This module removes that limit, allowing stepless ratio adjustment in both landscape and portrait.

## Features

- Stepless split-screen ratio: drag the divider to any position and release — works in both landscape and portrait

## Compatibility

| Item | Description |
| --- | --- |
| Device | Red Magic 8S Pro+ (NX729J) ✅ tested |
| OS | RedMagicOS 10.0.12 (Android 15) |
| Framework | LSPosed (Zygisk / KernelSU) |
| Other Red Magic models | ⚠️ Not tested, unknown |

## Installation

1. Download the latest APK from [Releases](../../releases) and install it
2. Open LSPosed → Modules → enable SplitFix
3. Check scope: **System UI** (`com.android.systemui`)
4. Tap "Restart SystemUI"

## Build

```bash
bash build.sh
# Output: build/SplitFix-v1.3.apk
```

Requires Android SDK (build-tools 35, platforms/android-35) and JDK 17+.

## How it works

Hooks the divider snap calculation (`DividerSnapAlgorithm.calculateSnapTarget`): when the divider is inside the screen, it stays where you release it (stepless adjustment).

Pure AI-built project, developed entirely by DeepSeek V4 Flash + Hermes Agent.

## Disclaimer

This module is for learning and communication purposes only. Use at your own risk; back up important data before use.

## License

[MIT](./LICENSE)
