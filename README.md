# Skyline App

Native Android companion to the Skyline weather website. See [the roadmap](PLAN.md) for the release plan. This first build is for testing widget placement on the Galaxy S24 Ultra and Galaxy Tab S9 FE.

The app and the widget show one shared sample for Saarbrücken. **Change sample weather** switches between **19°C · Clear sky** and **12°C · Light rain**. The app stores the selection on the device and asks Android to redraw any Skyline widgets. The prototype uses fixed samples and works offline.

## Emulator verification

Verified on 10 November 2026 on the stock Android 16 emulator (build `BE2A.250530.026.D1`), using phone and tablet display profiles.

- Installed the signed debug APK and opened the app on both display profiles.
- Found **Skyline weather** in the launcher widget picker and added it to the phone home screen. Expanded the widget horizontally and confirmed its labels stayed visible.
- Tapped the widget to open Skyline. Changed the sample and confirmed the existing widget updated to **12°C · Light rain** without re-adding it.
- Confirmed the app and widget kept the selected sample in tablet portrait and landscape. Switched back to **19°C · Clear sky** and confirmed the tablet home widget updated.
- Android lint completed with zero errors and five warnings about newer build tools and libraries, keyboard and mouse support, and backup configuration.

The local APK and screenshots are in `artifacts/`. The debug package is `com.skyline.weather.prototype`.

## Galaxy lock screen

The emulator runs stock Android. One UI Home, Samsung's lock-screen picker, Good Lock's LockStar and Always On Display are pending on Galaxy hardware. [Samsung's Remote Test Lab](https://developer.samsung.com/remote-test-lab) can provide remote Galaxy access with a Samsung Developer account, subject to device and feature availability. [Samsung's emulator skin guide](https://developer.samsung.com/galaxy-emulator-skin/guide.html)

For a Galaxy check, install the debug APK, add **Skyline weather** from the home-screen widget picker, then try the built-in lock-screen picker and **Good Lock → LockStar**. Check Always On Display separately where offered. Record the Android, One UI and LockStar versions with the results.

## Build locally

Open this folder in Android Studio, or use a JDK and the Android SDK from the command line. The prototype was built with JDK 21, Android SDK Platform 36 and Build Tools 35.0.0. Set `ANDROID_HOME` to the SDK path if Android Studio has not configured the project. The minimum supported OS is Android 12.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk` can install it on a connected device with USB debugging already enabled. An in-place update requires the same signing key.
