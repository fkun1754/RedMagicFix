**English**（[中文](./README.md)）

# RedMagic Fix

> An Xposed module for Red Magic 8S Pro+: stepless split-screen, unrestricted small windows, app cloning for any app, phone vibration for non-rumble gamepads, and the Red Magic key passed through to games.

On Red Magic 8S Pro+ (NX729J) the split-screen divider only snaps to 1:1, small windows only accept a system whitelist of apps, and app cloning only offers a handful of fixed apps. This module removes all three restrictions at runtime without modifying any system APK; disabling the module restores stock behavior.

## Features

- Stepless split-screen ratio: drag the divider to any position and release — works in both landscape and portrait
- Any app in small window: both normal-mode and in-game small windows accept any third-party app
- Any app can be cloned: the app-cloning list shows every installed third-party app
- Red Magic key pass-through: in-game the key no longer opens the game assistant; edge-swipe still works
- Phone vibration: games that rumble a motorless gamepad vibrate the phone instead (Headup)
- Settings screen: every fix can be toggled individually, effective immediately

## Compatibility

| Item | Description |
| --- | --- |
| Device | Red Magic 8S Pro+ (NX729J) ✅ tested |
| OS | RedMagicOS 10.0.12 (Android 15) |
| Framework | LSPosed (Zygisk / KernelSU) |
| Other Red Magic models | ⚠️ Not tested, unknown |

## Installation

1. Download the latest APK from [Releases](../../releases) and install it
2. Open LSPosed → Modules → enable RedMagic Fix
3. Select all 7 scopes:
   - System UI `com.android.systemui`
   - System Framework `android`
   - App Cloning `com.zte.cn.doubleapp`
   - Floating Window `com.zte.floatassist`
   - Game Float `cn.zte.gamefloat`
   - Game Space `cn.nubia.gamelauncher`
   - Game Assist `cn.nubia.gameassist`
   - Headup `com.headup.epic.smbf`
4. Reboot the device

After installing, the **RedMagic Fix** icon appears in the launcher; grant "Modify system settings" to toggle each fix.

## Build

```bash
bash build.sh
# Output: build/RedMagicFix-v1.6.apk
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
```

Requires Android SDK (build-tools 35, platforms/android-35) and JDK 17+.

## How it works

- Split screen: hooks `DividerSnapAlgorithm.calculateSnapTarget` so the divider stays wherever it is released inside the screen
- Small window: hooks `ActivityClientController.checkTaskSupportWr` and `TaskDisplayAreaMifavor.checkTaskSupportForCompWr`, with per-app fallbacks; normal-mode and in-game small windows share this check
- App cloning: hooks `UpdateUtils.getSupportApps` and returns every installed third-party app with a launcher icon
- Red Magic key: hooks `InputManagerService.interceptKeyBeforeQueueing` so the intercepted `BTN_MODE` falls through to the normal window policy
- Vibration: hooks `InputDevice.getVibrator()` and returns the phone vibrator when the gamepad has none

## Known limitations

- Only third-party apps are unlocked for cloning; system apps stay hidden
- The 15-app cap of the floating window list is unchanged
- Some apps do not handle freeform resizing well and may show black bars, layout glitches or crash
- Banking and payment apps may refuse to run inside a clone for their own security reasons

Pure AI-built project, developed entirely by DeepSeek V4 Flash + Hermes Agent; version 1.4 iterated by Codex.

## Disclaimer

This module is for learning and communication purposes only. Use at your own risk; back up important data before use.

## License

[MIT](./LICENSE)
