**语言：中文**（[English](./README_EN.md)）

# SplitFix

> 为红魔 8S Pro+ 解除分屏比例限制，改为无级调节的 Xposed 模块。

红魔 8S Pro+（NX729J）横屏分屏时，分隔条只能停在 1:1，无法调节比例。本模块解除该限制，横竖屏均可无级调节分屏比例，并保留系统「拖到边缘关闭分屏」功能。

## 功能

- **无级调节**：分隔条拖到任意位置松手即停，横屏竖屏均生效
- **保留官方手势**：拖到屏幕边缘关闭分屏，行为不变
- **零侵入**：不修改任何系统文件，停用模块即完全还原

## 兼容性

| 项目 | 说明 |
| --- | --- |
| 设备 | 红魔 8S Pro+（NX729J）✅ 已实测 |
| 系统 | RedMagicOS 10（Android 15） |
| 框架 | LSPosed（Zygisk / KernelSU 均可） |
| 其他红魔机型 | ⚠️ 未测试，可用性未知 |

## 安装

1. 在 [Releases](../../releases) 下载最新 APK 并安装
2. 打开 LSPosed → 模块 → 启用 SplitFix
3. 作用域勾选「系统界面」（com.android.systemui）
4. 点击「重启 SystemUI」

## 构建

```bash
bash build.sh
# 输出：build/SplitFix-v1.3.apk
```

依赖：Android SDK（build-tools 35、platforms/android-35）、JDK 17+。

## 工作原理

Hook 系统分屏的停靠点计算（`DividerSnapAlgorithm.calculateSnapTarget`）：分隔条位于屏幕内时直接停靠当前位置，实现无级调节；拖出屏幕边缘时放行系统原逻辑，保留关闭分屏。

纯 AI 构建项目，由 DeepSeek V4 Flash + Hermes Agent 全程开发完成。

## 免责声明

本模块仅供学习交流使用。使用过程中造成的任何后果由使用者自行承担，请提前备份重要数据。

## License

[MIT](./LICENSE)
