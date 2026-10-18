**English**（[中文](./README.md)）

# RedMagic Fix

> An Xposed module for Red Magic 8S Pro+, split into **phone tweaks** and **gamepad tweaks**. Everything is a runtime hook; no system APK is modified and disabling a switch restores stock behaviour.

Red Magic 8S Pro+ (NX729J) ships with a few limits: the split-screen divider only snaps to 1:1, small windows only accept a system whitelist, app cloning only offers a handful of apps, and the GameSir Red Magic gamepad has no rumble motor while its Home key is intercepted by the system to open the game assistant. This module lifts them one by one.

## Features

### Phone tweaks

- Stepless split-screen ratio: drag the divider anywhere and release — works in both landscape and portrait
- Any app in small window: both normal-mode and in-game small windows accept any third-party app
- Any app can be cloned: the app-cloning list shows every installed third-party app

### Gamepad tweaks

- Home key pass-through: in-game the gamepad Home key (`BTN_MODE`) is no longer intercepted to open the game assistant, and instead reaches the game; edge-swipe still opens the panel
- Rumble to phone: the gamepad has no motor, so game rumble vibrates the phone instead (currently for Headup)

Every feature can be toggled individually on the settings screen, effective immediately.

## Compatibility

| Item | Description |
| --- | --- |
| Device | Red Magic 8S Pro+ (NX729J) ✅ tested |
| OS | RedMagicOS 10.0.12 (Android 15) |
| Gamepad | GameSir Red Magic Shadow Blade Gamepad 2 (VID 3537 / PID 1109, generic HID) ✅ tested |
| Framework | LSPosed (Zygisk / KernelSU) |
| Other Red Magic models | ⚠️ Not tested, unknown |

## Installation

1. Download the latest APK from [Releases](../../releases) and install it
2. Open LSPosed → Modules → enable RedMagic Fix
3. Select all 8 scopes:
   - System UI `com.android.systemui`
   - System Framework `android`
   - App Cloning `com.zte.cn.doubleapp`
   - Floating Window `com.zte.floatassist`
   - Game Float `cn.zte.gamefloat`
   - Game Space `cn.nubia.gamelauncher`
   - Game Assist `cn.nubia.gameassist`
   - Headup `com.headup.epic.smbf`
4. Reboot the device

After installing, the **RedMagic Fix** icon appears in the launcher; grant "Modify system settings" to toggle each feature.

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

### Phone tweaks

- Split screen: hooks `DividerSnapAlgorithm.calculateSnapTarget` so the divider stays wherever it is released inside the screen
- Small window: hooks `ActivityClientController.checkTaskSupportWr` and `TaskDisplayAreaMifavor.checkTaskSupportForCompWr`, with per-app fallbacks; normal-mode and in-game small windows share this check
- App cloning: hooks `UpdateUtils.getSupportApps` and returns every installed third-party app with a launcher icon

### Gamepad tweaks

- Home key: hooks `InputManagerService.interceptKeyBeforeQueueing` so the `BTN_MODE` intercepted in game mode falls through to the normal window policy; edge-swipe is untouched
- Rumble: hooks `InputDevice.getVibrator()` and returns the phone vibrator when the gamepad has none

## Known limitations

- Only third-party apps are unlocked for cloning; system apps stay hidden
- The 15-app cap of the floating window list is unchanged
- Some apps do not handle freeform resizing well and may show black bars, layout glitches or crash
- Banking and payment apps may refuse to run inside a clone for their own security reasons
- Home key pass-through only applies to apps that are in a game scene when the key is pressed
- A game must actually request rumble from the gamepad for "rumble to phone" to trigger

Pure AI-built project, developed entirely by DeepSeek V4 Flash + Hermes Agent; iterated by Codex since 1.4.

## Disclaimer

This module is for learning and communication purposes only. Use at your own risk; back up important data before use.

## License

[MIT](./LICENSE)
