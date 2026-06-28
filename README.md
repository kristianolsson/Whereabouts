# Whereabouts

A lightweight macOS menu bar app that shows a flag emoji for the country of your current public IP address. Connect to a VPN and the flag updates automatically.

## How it works

- Watches for network changes using `NWPathMonitor` — no polling timer
- When the network path changes (VPN connect/disconnect, interface change), it waits ~1.5s for the route to settle, then queries the geolocation API
- Falls back to [ip-api.com](http://ip-api.com) if the primary [ipinfo.io](https://ipinfo.io) lookup fails
- Last known country is cached in `UserDefaults` so the correct flag shows instantly on launch
- A backstop poll runs every 30 minutes in case a path-change event is missed

## Menu

Click the flag in the menu bar to see:
- Current city and country (with IP)
- Last updated time
- **Refresh Now** — manual re-lookup

## Requirements

- macOS 13 Ventura or later

## Building

```bash
swift build
.build/debug/Whereabouts
```

For a release build:

```bash
swift build -c release
.build/release/Whereabouts
```
