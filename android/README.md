# Whereabouts — Android

Shows a persistent country flag emoji in the Android status bar, matching the
country of the device's current public IP address. When you connect or
disconnect a VPN the flag updates automatically (within ~5 s).

## How it works

| Component | Role |
|---|---|
| `FlagService` | Foreground Service — holds the ongoing notification, registers a `BroadcastReceiver` for `CONNECTIVITY_ACTION`, debounces network changes (5 s), then calls the geo APIs |
| `NetworkMonitor` | `BroadcastReceiver` for `ConnectivityManager.CONNECTIVITY_ACTION`; more reliable than `NetworkCallback` on Samsung devices in background |
| `GeoLocator` | HTTP GET → `https://ipinfo.io/json`, fallback to `http://ip-api.com/json`; no API key needed |
| `FlagEmoji` | ISO 3166-1 alpha-2 code → Unicode flag emoji via Regional Indicator Symbols |
| `BootReceiver` | Restarts `FlagService` on `ACTION_BOOT_COMPLETED` |
| `Prefs` | SharedPreferences cache: persists last country code / IP so the correct flag appears instantly on restart |
| `MainActivity` | Shows current flag, IP, country code, last-updated time, on/off toggle, and a Refresh button |

An `AlarmManager` backstop re-runs geolocation every 30 minutes in case a
network event is missed. If the initial lookup fails (network not yet settled),
it retries up to 3 times at 8-second intervals.

## Requirements

- Android 8.0+ (API 26+)
- Internet permission (for geo lookup)

## Build

```bash
cd android/
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew assembleDebug
```

The debug APK lands at:
```
app/build/outputs/apk/debug/app-debug.apk
# also copied to:
app/build/whereabouts-debug.apk
```

## Sideload to a device

1. Enable **Developer options** on the device (tap *Build number* 7 times).
2. Enable **USB debugging** in Developer options.
3. Connect via USB and accept the authorisation prompt on the device.
4. Install:
   ```bash
   adb install -r android/app/build/whereabouts-debug.apk
   ```
5. Open **Whereabouts** from the launcher. Grant the notification permission
   when prompted. The flag appears in the status bar within a few seconds.

To install wirelessly (Android 11+):
```bash
adb pair <ip>:<port>   # pair code shown in Developer options > Wireless debugging
adb connect <ip>:<port>
adb install -r android/app/build/whereabouts-debug.apk
```
