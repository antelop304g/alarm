# Alarm

Android default SMS app that escalates parking-enforcement SMS alerts by matching configured keywords such as `闽AF1234`.

## Key Features

- Requests the default SMS app role and blocks setup until granted.
- Stores incoming SMS into the Telephony provider, observes conversations, and supports manual sending.
- Matches configured keywords in SMS bodies, then starts a foreground alarm with vibration, fullscreen intent, and a manual stop action.
- Includes HyperOS-focused setup guidance for Xiaomi devices to reduce background restrictions.

## Local Prerequisites

- JDK 17 or newer
- Android SDK platform 35 and build tools
- Android command-line tools or Android Studio

## Build

```bash
./gradlew assembleDebug
```
