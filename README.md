<div align="center">
<img src="assets/jetbird_banner_full_rev4.svg" alt="JetBird banner" />

[![status-badge](https://ci.codeberg.org/api/badges/status.svg)]()
[![Submodule SHA](https://badges.codeberg.org/badge/dynamic/json?url=https%3A%2F%2Fcodeberg.org%2Fapi%2Fv1%2Frepos%2Fbg443%2FJetBird%2Fcontents%2Fnetbird&query=%24.sha&label=submodule%20commit%20SHA)]()
[![Upstream Release](https://badges.codeberg.org/github/v/release/netbirdio/netbird?label=latest%20upstream%20release)](https://github.com/netbirdio/netbird/releases/latest)
[![GitHub commits since latest release](https://badges.codeberg.org/github/commits-since/netbirdio/netbird/latest)](https://github.com/netbirdio/netbird)
<p>An unofficial, FLOSS, native Android NetBird client built with privacy in mind</p>
</div>

## About
JetBird is an implementation of the NetBird client for Android.

It has been built with privacy in mind and contains no blobs or trackers.

## Features
- Native UI
- Use SSO or setup keys
- Use pre-shared keys
- See all logs in real time
- Quick tile
- Exclude apps from the tunnel
- Exit nodes (with client-side customisation)
- Tablet support

## Differences
There are a few differences between NetBird official Android client and JetBird. Some of these features rely on patches to the NetBird source (these are in `patches/`):
- See all peer info
- Use setup keys even with SSO support
- Rosenpass support
- Split tunneling via routes and/or per app
- l18n

## Install
### Prebuilt binaries
- Available to download in the [releases](https://codeberg.org/bg443/JetBird/releases) tab
- You can see the steps of the build on [JetBird's CI page]()
- Where possible, download the specific build for your device's architecture because the NetBird client is ~35-40MB and downloading the universal version will download all 4 architectures regardless of if you need them

<br />
<div>
    <a href="https://codeberg.org/bg443/JetBird/releases" rel="noopener noreferrer" target=_blank><img src="./assets/codeberg.svg" height=60 alt="Get it on Codeberg" /></a>&nbsp;&nbsp;&nbsp;
    <a href="https://github.com/ImranR98/Obtainium" rel="noopener noreferrer" target="_blank"> <img src="./assets/badge_obtainium.png" height="60" alt="Get it on Obtainium" /></a>&nbsp;&nbsp;&nbsp;
    <a href="https://codeberg.org/bg443/JetBird/releases.rss" rel="noopener noreferrer" target=_blank><img src="./assets/RSS.svg" height=60 alt="Follow with RSS" /></a>
</div>

### Build from source

1. Build NetBird
```
./scripts/build_netbird.sh
```
JetBird's NetBird client patches will be applied automatically.

2. Build APK
```
./gradlew :app:assembleRelease
```

## Privacy
JetBird does not, and never will, call home.

JetBird/NetBird has to talk to your management server, which will have it's own privacy policy.

In the NetBird management portal, admins will be able to see:
- You are using JetBird
- JetBird and NetBird versions
- Device model e.g. Pixel 9 Pro Fold
- Operating system e.g. Android 14

JetBird makes no extra connections or requests outside of the NetBird client.

## Permissions
JetBird requires a few permissions in order to function:
- `INTERNET`
- `FOREGROUND_SERVICE`/`FOREGROUND_SERVICE_SYSTEM_EXEMPTED` - used to run the VPN service
- `ACCESS_NETWORK_STATE`/`CHANGE_NETWORK_STATE` - used to monitor network to check if VPN is already running etc.
- `QUERY_ALL_PACKAGES` - used so you can choose which apps aren't tunneled

## Screenshots
### Phone
<div align="center">
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="" loading="lazy" width="20%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="" loading="lazy" width="20%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="" loading="lazy" width="20%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/4.png" alt="" loading="lazy" width="20%" />
<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" alt="" loading="lazy" width="20%" />
</div>

### Tablet
<div align="center">
<img src="fastlane/metadata/android/en-US/images/tenInchScreenshots/1.png" alt="" loading="lazy" width="50%" />
<img src="fastlane/metadata/android/en-US/images/tenInchScreenshots/2.png" alt="" loading="lazy" width="50%" />
<img src="fastlane/metadata/android/en-US/images/tenInchScreenshots/3.png" alt="" loading="lazy" width="50%" />
<img src="fastlane/metadata/android/en-US/images/tenInchScreenshots/4.png" alt="" loading="lazy" width="50%" />
</div>

## Issues
As JetBird is just a wrapper around the official NetBird client, most connection/peer/management issues will reside with upstream.

Please open an issue using the template provided if you believe you have found a bug or have a feature request!

## Libraries

See a full list of libraries used [here](https://codeberg.org/bg443/JetBird/wiki/Libraries)

## License
<img src="assets/gpl_v3.svg" height=100 />
<br />
JetBird is free software: you can redistribute it and/or modify it under the terms of the [GNU GPL-3.0-or-later License](./LICENCE.md) as published by the Free Software Foundation.

This software is provided with no warranty.

## Other
"NetBird" and the "NetBird" logo are <a href="https://netbird.io/terms" rel="noopener noreferrer" target=_blank>copyright</a> owned by or licensed to Wiretrustee UG.

"WireGuard" and the "WireGuard" logo are registered <a href="https://www.wireguard.com/trademark-policy" rel="noopener noreferrer" target=_blank>trademarks</a> of Jason A. Donenfeld.
