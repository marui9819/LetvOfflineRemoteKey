# 屏霸 TV 键离线配对工具

当前版本：1.1.1（versionCode 3）

这是一个独立 Android 应用，用于在 LeTV 屏霸 SHC Air 上离线选择电视品牌/红外电源码组，并通过屏霸系统服务写入遥控器 TV 键。应用不请求网络，也不依赖电脑端服务。

最低 Android 版本为 6.0（API 23）。界面启动时只读取 `android.letvremoteupgrademanager` 服务并显示连接状态。只有用户选择品牌和候选码组、点击写入并在确认框中再次确认后，应用才发送一次配置事务。写入 ACK 仅表示屏霸服务接受了码组；用户仍需手动将遥控器对准电视按 TV 键验证。

已验证可用的 SHC Air 乐视码始终保留在应用中，可用于恢复。另随 APK 离线附带 14 个其他品牌共 34 组候选电源码（创维、飞利浦、海信、海尔、长虹、三星、康佳、夏普、TCL、小米、LG、东芝、松下、日立）。候选码是码库中的遥控器配置标识，不保证与品牌下的每一款电视兼容；应逐个尝试，并以实际 TV 键测试为准。

屏霸系统没有经本工具确认的遥控器当前码回读接口，因此写入前无法备份 TV 键旧码。请在确认框中确认覆盖后再继续。应用不发送电视开关机红外信号，不会在启动时写码，也不提供刷机、升级遥控器固件或 root 功能。

除乐视内置码外，候选码来自 [flipperdevices/IRDB](https://github.com/flipperdevices/IRDB)，固定在 revision `f7b15366521cc81ba11b341538f0097bddbb748b`，按该仓库 MIT License 随 APK 附带；许可证见 `app/src/main/assets/IRDB-NOTICE.txt`。可用 `python3 tools/import_ir_profiles.py` 重建离线码库。

## 许可

本工具自身的源代码尚未单独声明开源许可证；公开仓库不代表自动授予复制、修改或再分发许可。随附的 IRDB 数据按其上游 MIT License 和 NOTICE 文件处理。

## 构建

需要 Android SDK 34、Java 17 和 Gradle Wrapper：

```bash
./gradlew assembleDebug
```

APK：`app/build/outputs/apk/debug/app-debug.apk`

## 当前验证范围

- 已构建 debug APK。
- 2026-10-24 已安装到 SHC Air 并启动，启动页实际显示“已连接屏霸服务”，读取到接口描述符 `android.LetvRemoteUpgradeService`。
- 服务连接检查仅调用 `ServiceManager.getService` 并读取 Binder 接口描述，不执行配置事务。
- 用户已确认写入乐视码成功，屏霸遥控器的电视开关键可以工作。
- 多品牌候选码版本的源码/UI/离线资产已准备，打包后不代表其中每个品牌都已在实机验证。
- 品牌和码组选项已改为高对比遥控器焦点列表：焦点项为亮蓝底/白色粗边框，已选项带“✓ 当前选择”；已在 SHC Air 上用 D-pad 验证焦点移动。
