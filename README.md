# Skyline App

Native Android companion to the Skyline weather website. See [the roadmap](PLAN.md) for the release plan. The forecast build adds city search, saved cities and a three-day outlook for the Galaxy S24 Ultra and Galaxy Tab S9 FE.

## Home-screen widgets

- **Skyline Compact** shows current sample conditions and temperature in a 2 × 1 default tile.
- **Skyline Hourly** shows sample temperatures and rain chances in a 4 × 2 default tile. It shows up to four hourly slots depending on the available space and uses the compact view in especially tight placements.

Both use the same Saarbrücken sample and switch from a sunny blue tint to a slate rain tint. Tap a tile to open **Widget sample**, or open it from the app. **Change sample weather** switches between **19°C · Clear sky** and **12°C · Light rain** and refreshes both tiles. The sample widgets work offline without API setup.

The local `artifacts/Skyline-Widgets-Demo.apk` is the standalone debug build for this step. Install it, then add the tiles from the home-screen widget picker under **Skyline**.

The stock Android 16 phone profile verified both picker entries and kept an existing Compact tile through the update. Hourly reflowed from four slots to three when narrowed. With Wi-Fi and mobile data disabled, switching to the rainy sample refreshed both tiles in place. Both layouts also rendered on the stock Android 16 landscape tablet profile.

## Forecast build

The Android app shows current conditions, the next 12 available forecast hours with rain chances, and up to three forecast days. It stores up to eight cities, remembers the selected city, and formats times in the selected city's time zone. On wider displays the outlook appears alongside current and hourly weather.

Saved weather can be shown offline within its age limits. Current conditions expire after one hour and the hourly and daily outlook after 24 hours, measured from the server's fetch time. The app discards expired entries while in use and shows when the current conditions were observed. The home-screen widgets use sample data.

### Run the emulator build

Use Node.js 24 for the local service. Run these commands in separate terminals from the repository root.

```sh
cd server
npm test
npm run fixture
```

```sh
./gradlew -PskylineWeatherBaseUrl=http://10.0.2.2:8765 :app:assembleDebug :app:lintDebug
```

Install the APK in the Android emulator. Search for **Springfield** to see two distinct results, or **Saarbrucken** for the Germany fixture. The fixtures are synthetic. The local `artifacts/Skyline-Forecast-Emulator.apk` is preconfigured for this service. The emulator-only HTTP configuration is excluded from release builds.

### Forecast verification

The stock Android 16 emulator resolved two Springfields by region, saved Springfield and Saarbrücken, and displayed their local forecast times. The phone used one column and the tablet used two. After the fixture server stopped, a restart restored the saved cities and forecast. Advancing the emulator clock confirmed that current conditions disappear after an hour and the outlook after a day. The server checks passed all six cases, and Android lint reported no issues.

### Connect live WeatherAPI.com data

1. Configure a WeatherAPI credential authorized for the mobile service as `WEATHERAPI_KEY` in `server/.env` for local development or in the deployment's environment settings. Run `npm run dev` from `server/` for local testing.
2. For hosted use, deploy the `server/` directory as a Vercel project using Node.js 24. The versioned routes are `GET /api/v1/search?q=…` and `GET /api/v1/forecast?id=…`. The server caches matching requests for five minutes and limits upstream calls per process. Configure a shared rate limit before opening the endpoint publicly, since the function can run across multiple instances.
3. Build Android with `-PskylineWeatherBaseUrl=https://<mobile-api-host>`. The Android build takes the service origin and the provider key stays on the server.

[WeatherAPI documentation](https://www.weatherapi.com/docs/) · [WeatherAPI terms](https://www.weatherapi.com/terms) · [Vercel Node.js functions](https://vercel.com/docs/functions/runtimes/node-js)

## Widget prototype verification

On 10 November 2026, stock Android 16 (build `BE2A.250530.026.D1`) accepted the widget from the picker, resized it horizontally, launched Skyline on tap, and redrew the same tile after the sample changed in phone and tablet layouts. The sample APK and screenshots are in `artifacts/`. The debug package is `com.skyline.weather.prototype`.

## Galaxy lock screen

The emulator runs stock Android. One UI Home, Samsung's lock-screen picker, Good Lock's LockStar and Always On Display are pending on Galaxy hardware. [Samsung's Remote Test Lab](https://developer.samsung.com/remote-test-lab) can provide remote Galaxy access with a Samsung Developer account, subject to device and feature availability. [Samsung's emulator skin guide](https://developer.samsung.com/galaxy-emulator-skin/guide.html)

For a Galaxy check, install the debug APK, add **Skyline Compact** or **Skyline Hourly** from the home-screen widget picker, then try the built-in lock-screen picker and **Good Lock → LockStar**. Check Always On Display separately where offered. Record the Android, One UI and LockStar versions with the results.

## Build locally

Open this folder in Android Studio, or use a JDK and the Android SDK from the command line. The builds use JDK 21, Android SDK Platform 36 and Build Tools 35.0.0. Set `ANDROID_HOME` to the SDK path if Android Studio has not configured the project. The minimum supported OS is Android 12.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

Set the service origin as described above to enable forecast requests. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Android Studio or `adb install -r app/build/outputs/apk/debug/app-debug.apk` can install it on a connected device with USB debugging already enabled. An in-place update requires the same signing key.
