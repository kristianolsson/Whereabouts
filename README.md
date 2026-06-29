# Whereabouts

Shows a flag for the country of your current public IP. Connect to a VPN and the flag updates automatically — no polling, event-driven via network change callbacks.

## Platforms

| Platform | Location | Tech |
|----------|----------|------|
| macOS | [`macos/`](macos/) | Swift, NSStatusItem, NWPathMonitor |
| Android | [`android/`](android/) | Kotlin, Foreground Service, ConnectivityManager |

## How it works

- Watches for network interface changes (VPN connect/disconnect triggers an update)
- Queries [ipinfo.io](https://ipinfo.io/json) for the current public IP's country, with [ip-api.com](http://ip-api.com/json) as fallback
- Converts the ISO country code to a flag emoji using Unicode regional indicator scalars
- Caches the last known country so the correct flag shows instantly on launch/start

## macOS

See [`macos/README.md`](macos/README.md) for build and installation instructions.

## Android

See [`android/README.md`](android/README.md) for build and sideload instructions.
