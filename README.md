**语言：中文**（[English](./README_EN.md)）

# RedMagic Fix

> 红魔 8S Pro+ 限制解除模块，分「手机功能」与「手柄功能」两部分，全部为运行时 Hook，不改动任何系统 APK，关闭开关即恢复原状。

红魔 8S Pro+（NX729J）上有几处限制：横屏分屏分隔条只能停在 1:1；小窗只允许系统白名单里的应用打开；应用分身只能选固定的几个 App；GameSir 红魔手柄没有马达、Home 键在游戏里会被系统拦去唤出游戏助手。本模块把它们逐一解除。

## 功能

### 手机功能修改

- 分屏比例无级调节：分隔条拖到任意位置松手即停，横屏、竖屏均生效
- 小窗不限应用：常规小窗与游戏模式小窗都能打开任意第三方 App
- 应用分身不限应用：分身列表显示所有已安装的第三方 App

### 手柄功能修改

- 手柄 Home 键放行：手柄 Home 键（`BTN_MODE`）在游戏里不再被系统拦去唤出游戏助手，改为完整交给游戏；侧滑唤出面板不受影响

所有功能都可以在设置页里单独开关，修改后即时生效。模块默认不显示桌面图标，可在设置页打开「显示桌面图标」；没图标时从快捷设置面板的「RedMagic Fix」磁贴进入设置（也可以 `adb shell am start -n com.redmagic.fix/.SettingsActivity`）。

> Android 15 禁止普通应用向 `Settings.System` 写入自定义键，因此首次修改开关时会请求 root（KernelSU/Magisk 授权）来写入配置，仅此一处需要 root。

## 兼容性

| 项目 | 说明 |
| --- | --- |
| 设备 | 红魔 8S Pro+（NX729J）✅ 已实测 |
| 系统 | RedMagicOS 10.0.12（Android 15） |
| 手柄 | GameSir 红魔 Shadow Blade Gamepad 2（VID 3537 / PID 1109，通用 HID）✅ 已实测 |
| 框架 | LSPosed（Zygisk / KernelSU 均可） |
| 其他红魔机型 | ⚠️ 未测试，可用性未知 |

## 安装

1. 在 [Releases](../../releases) 下载最新 APK 并安装
2. 打开 LSPosed → 模块 → 启用 RedMagic Fix
3. 作用域勾选以下 3 项：
   - 系统界面 `com.android.systemui`
   - 系统框架 `android`
   - 应用分身 `com.zte.cn.doubleapp`
4. 重启手机

安装后桌面会出现 **RedMagic Fix** 图标，打开后先授予「修改系统设置」权限，即可逐项开关。

可选作用域（默认不勾选）：

- 常规小窗 / 游戏浮窗 / 游戏空间 / 游戏助手：仅在小窗功能异常时作为兜底

## 构建

```bash
bash build.sh
# 输出：build/RedMagicFix-v1.6.apk
```

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1
```

依赖：Android SDK（build-tools 35、platforms/android-35）、JDK 17+。

## 工作原理

### 手机功能

- 分屏：Hook 系统分屏的停靠点计算（`DividerSnapAlgorithm.calculateSnapTarget`），分隔条位于屏幕内时直接停靠当前位置
- 小窗：Hook 系统的小窗支持判断（`ActivityClientController.checkTaskSupportWr`、`TaskDisplayAreaMifavor.checkTaskSupportForCompWr`），并在各小窗 App 内做兜底；常规小窗与游戏模式小窗共用这一处判断
- 分身：Hook 应用分身的支持列表（`UpdateUtils.getSupportApps`），返回所有带桌面图标的第三方 App

### 手柄功能

- Home 键：Hook 系统的 `InputManagerService.interceptKeyBeforeQueueing`，把游戏场景下被 ZTE 拦截的 `BTN_MODE` 交还给窗口策略链路，侧滑唤出面板不受影响

## 已知限制

- 应用分身只放开第三方 App，系统 App 不会出现在列表里
- 常规浮窗列表最多 15 个 App 的上限未改动
- 部分 App 自身不适配自由窗口，可能出现黑边、界面错位或闪退
- 银行、支付类 App 可能因自身安全策略拒绝在分身环境运行
- 手柄 Home 键放行只对按下时处于游戏场景的 App 生效

纯 AI 构建项目，由 DeepSeek V4 Flash + Hermes Agent 全程开发，1.4 起由 Codex 迭代。

## 免责声明

本模块仅供学习交流使用。使用过程中造成的任何后果由使用者自行承担，请提前备份重要数据。

## License

[MIT](./LICENSE)
