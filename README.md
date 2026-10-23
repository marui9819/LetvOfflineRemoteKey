# 屏霸 TV 键离线配对工具

[下载 APK 1.1.1](https://github.com/marui9819/LetvOfflineRemoteKey/releases/download/v1.1.1/LetvOfflineRemoteKey-1.1.1-debug.apk) · [全部版本](https://github.com/marui9819/LetvOfflineRemoteKey/releases) · [源码](https://github.com/marui9819/LetvOfflineRemoteKey)

如果这个小工具帮你恢复了屏霸遥控器的电视开关键，欢迎点一下 **Star**，也欢迎反馈新的电视型号和实际测试结果。

独立 Android 工具，用于在乐视智能家庭中心“屏霸”SHC Air 上离线选择电视品牌/红外电源码组，并将所选配置写入屏霸系统服务。当前版本 `1.1.1`（versionCode `3`），最低 Android 6.0/API 23。

## 主要特点

- 全程离线，不请求网络，也不需要电脑端服务。
- 已验证的 SHC Air 乐视码始终保留，其他品牌附带 34 组候选码（14 个品牌），可逐组尝试。
- 只有用户选择品牌和候选码组、点击写入并再次确认后，才向系统服务写入一次配置事务。
- 启动时只读取系统服务连接状态；不会自动写入，也不会主动发射电视开关机红外信号。
- 高对比度遥控器焦点、清晰的已选项标记和写入前确认。

该工具通过系统服务设置屏霸遥控器的 TV 键码组。服务返回 ACK 只表示配置事务被接受；请将遥控器对准电视实际验证。候选码是码库配置标识，不保证适用于同品牌每个型号。除乐视配置外，候选码尚未逐电视型号实测。

## 下载与安装

- [屏霸 TV 键离线配对工具 1.1.1 Debug APK](https://github.com/marui9819/LetvOfflineRemoteKey/releases/download/v1.1.1/LetvOfflineRemoteKey-1.1.1-debug.apk)
- [浏览所有 Release](https://github.com/marui9819/LetvOfflineRemoteKey/releases)

这是 Debug 侧载包，不是应用商店签名包。最低系统 Android 6.0/API 23。

## 使用步骤

1. 在屏霸上安装并打开 APK，确认显示已连接屏霸系统服务。
2. 选择电视品牌和码组；优先保留或选择已验证的乐视码。
3. 点击写入，在确认框中核对后确认。
4. 将屏霸遥控器对准电视，实际测试 TV 键。若无效，尝试该品牌的其他候选码组；需要时可重新写入乐视码恢复。

屏霸服务没有经本工具确认的当前码回读接口，因此写入前无法备份旧码。请确认后再覆盖。应用不提供 root、固件刷写或升级功能。

## 候选红外码与许可

除屏霸内置乐视码外，离线候选配置来自 [flipperdevices/IRDB](https://github.com/flipperdevices/IRDB)，固定在 revision `f7b15366521cc81ba11b341538f0097bddbb748b`，按上游 MIT License 随 APK 附带，说明见 `app/src/main/assets/IRDB-NOTICE.txt`。可使用 `python3 tools/import_ir_profiles.py` 重建候选码库。

本工具源码目前未单独声明开源许可证；公开仓库不等同于授予复制、修改或再分发许可。第三方 IRDB 内容按其上游许可证处理。

## 构建与验证

需要 Java 17、Android SDK 34 和仓库自带 Gradle Wrapper。运行 `./gradlew assembleDebug`；APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。

已在 SHC Air 上启动应用并确认可连接 `android.LetvRemoteUpgradeService`；乐视码写入后电视开关键已实测可用。D-pad 焦点移动已实机验证。其他品牌码组须由用户针对具体电视型号测试。

## English

LetvOfflineRemoteKey is an offline Android utility for choosing a TV-brand infrared profile for the TV key on the LeTV Smart Home Center (屏霸) SHC Air and writing that profile to its system service. Current version `1.1.1` (versionCode `3`); Android 6.0/API 23 or later.

### Highlights

- Fully offline: no network access and no computer-side service.
- Keeps the verified SHC Air LeTV profile and includes 34 candidate code groups across 14 other brands.
- Writes one configuration transaction only after the user selects a brand/profile, taps Write, and confirms.
- Startup only checks the system-service connection; it does not write automatically or transmit TV power IR commands.
- High-contrast D-pad focus, a visible selected state, and a confirmation step before writing.

The tool configures the remote's TV-key profile through the system service. An ACK means the configuration transaction was accepted; point the remote at the TV and test it physically. Candidate codes are database profile identifiers and are not guaranteed to match every model under a brand. Only the LeTV profile has been confirmed on the target setup; other profiles need model-specific testing.

### Download

- [LetvOfflineRemoteKey 1.1.1 Debug APK](https://github.com/marui9819/LetvOfflineRemoteKey/releases/download/v1.1.1/LetvOfflineRemoteKey-1.1.1-debug.apk)
- [All releases](https://github.com/marui9819/LetvOfflineRemoteKey/releases)

This is a debug sideload package, not a store-signed production build. Minimum Android version: 6.0/API 23.

### How to use

1. Install and open the APK on the SHC Air; verify that the system service is connected.
2. Choose the TV brand and candidate code group. Keep or select the verified LeTV profile first.
3. Tap Write and confirm the profile in the confirmation dialog.
4. Point the remote at the TV and test its TV key. If it does not work, try another candidate group; reapply the LeTV profile to restore the verified setup.

The tool has not confirmed a system API for reading back the current code, so the old TV-key profile cannot be backed up before writing. Review the confirmation before overwriting it. The app does not provide root, firmware flashing, or firmware updates.

### IR profiles and licensing

Except for the built-in LeTV profile, offline candidate data comes from [flipperdevices/IRDB](https://github.com/flipperdevices/IRDB), pinned to revision `f7b15366521cc81ba11b341538f0097bddbb748b` and included under its upstream MIT License. See `app/src/main/assets/IRDB-NOTICE.txt`. Rebuild the profile data with `python3 tools/import_ir_profiles.py`.

The utility source currently has no separate open-source license; public visibility does not grant permission to copy, modify, or redistribute it. Third-party IRDB material remains subject to its upstream license.

### Build and validation

Use Java 17, Android SDK 34, and the included Gradle Wrapper. Run `./gradlew assembleDebug`; the APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The app was launched on an SHC Air and connected to `android.LetvRemoteUpgradeService`. The LeTV profile was written and the TV power key was confirmed working. D-pad focus movement was also checked on the device. Other brand profiles still require testing against specific TV models.

## Star this project

If this utility helped restore your TV key, please **Star this repository**. Model-specific compatibility reports are welcome through [Issues](https://github.com/marui9819/LetvOfflineRemoteKey/issues).