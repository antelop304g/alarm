# MoveCarGuard

MoveCarGuard is an Android default SMS app focused on urgent move-car / parking-enforcement reminders.

It escalates parking-enforcement SMS alerts by matching configured keywords such as `闽AF1234`.

## 中文说明

MoveCarGuard（挪车短信卫士）是一个自用型 Android 默认短信 App，用来降低错过“抄牌挪车短信”的风险。

- App 会申请成为默认短信应用。
- 收到短信后会写入系统短信数据库，并在会话列表中展示。
- 当短信正文命中已配置关键字时，例如 `闽AF1234`，会立即触发前台告警。
- 告警支持铃声、震动、高优先级通知和全屏提醒。
- 内置 HyperOS/小米设备的设置引导，提醒用户完成默认短信、自启动、电池优化等配置。

### 适用场景

- 个人自用侧载安装
- 重点关注挪车、抄牌、停车类提醒短信
- 需要比普通通知更强的本地告警能力

### 使用前说明

- 首次启动后，需要把本应用设为默认短信 App。
- 在小米/HyperOS 设备上，建议同时开启自启动、关闭电池限制，并允许通知相关权限。
- 当前版本按“短信正文包含关键字”进行匹配，不做 OCR、不接第三方服务。

## Key Features

- Requests the default SMS app role and blocks setup until granted.
- Stores incoming SMS into the Telephony provider, observes conversations, and supports manual sending.
- Matches configured keywords in SMS bodies, then starts a foreground alarm with vibration, fullscreen intent, and a manual stop action.
- Includes HyperOS-focused setup guidance for Xiaomi devices to reduce background restrictions.

## Local Prerequisites

- JDK 17 or newer
- Android SDK platform 35 and build tools
- Android command-line tools or Android Studio

## 本地环境

- JDK 17 或更高版本
- Android SDK Platform 35
- Android Build Tools
- Android command-line tools 或 Android Studio

## Build

```bash
./gradlew assembleDebug
```

## 本地构建

```bash
./gradlew assembleDebug
```

调试包输出路径：

```text
app/build/outputs/apk/debug/app-debug.apk
```
