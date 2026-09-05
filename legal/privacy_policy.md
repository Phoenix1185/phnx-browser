# PHNX Browser Privacy Policy

Last updated: September 5, 2026

## Scope

PHNX Browser is a native Android application that uses the Android System WebView for browsing. This policy describes the current implementation; it is not a promise that every website or Android version behaves identically.

## Data stored locally

PHNX stores profile settings, tabs, sessions, bookmarks, history, download records, site permission decisions, privacy choices, identity settings, and network configuration in local app storage. Cookies, cache, Local Storage, IndexedDB, and other supported site data are managed by Android WebView for the active profile.

Private tabs are excluded from saved history and session restoration. Android System WebView does not provide a fully separate cookie partition for every private tab, so PHNX does not claim complete private-storage isolation.

## Information sent to other services

PHNX does not operate an analytics or advertising service in this build. Browsing a website sends information to that website. The update checker reads public release metadata from GitHub. Free proxy mode reads public proxy candidates from HProxy. A public proxy may inspect, modify, or record traffic and is not suitable for passwords, payments, or confidential information.

## Permissions

Camera, microphone, location, and notification access are requested only for implemented features and only through Android permission mechanisms. Network access is required for browsing and update checks. PHNX does not request contacts, SMS, phone, or device-administrator access.

## Choices and limitations

You can clear selected browsing data, clear history, remove bookmarks, reset site permissions, disable JavaScript or third-party cookies, disable proxy routing, and delete profiles from the app. PHNX is not a VPN, anonymity service, complete tracker-blocking engine, or security guarantee. Android WebView does not expose every desktop Chromium privacy or device-identity control.

## Contact

Project questions and privacy concerns can be reported through the repository issue tracker: https://github.com/Phoenix1185/phnx-browser/issues
