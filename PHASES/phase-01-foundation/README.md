# PHNX Browser — Phase 1

## Foundation + Chromium Browser Shell

> **Scope:** Build the first working Android version of PHNX Browser using a Chromium-based architecture. Phase 1 establishes the browser shell, navigation, tabs, basic menus, branding, settings, downloads, error handling, and CI artifacts without implementing the profile and network-isolation systems reserved for later phases.

## Objective

At the end of Phase 1, PHNX Browser should build successfully on Android, launch into a browser screen, display modern web pages with JavaScript enabled, provide a familiar mobile-browser interface, and produce APK and AAB artifacts through GitHub Actions.

## Technology decisions

| Area | Phase 1 decision |
|---|---|
| Language | Kotlin |
| Platform | Android |
| Browser engine | Chromium-based integration |
| Build system | Gradle |
| UI | Native Android UI |
| Source control | GitHub |
| Continuous integration | GitHub Actions |
| Application ID | `com.phoenix.phnx` (easy to change later) |
| Product name | PHNX Browser |
| Builder attribution | Built & engineered by Phoenix |

The project must remain a genuine Android application. It must not be migrated to Flutter, React Native, a web-only wrapper, or a WebView-only implementation. Application strings and branding belong in Android resources rather than being duplicated throughout the codebase.

## Phase 1 boundaries

The following capabilities are explicitly **out of scope** for Phase 1 and belong to later phases:

- Multiple isolated profiles and profile-specific persistence.
- Per-profile proxies and advanced network routing.
- Advanced fingerprint or device-configuration controls.
- Advanced RAM, CPU, lifecycle, and thermal management.
- Detailed privacy controls beyond correct baseline Android/browser permissions.

## Required application capabilities

| Capability | Phase 1 expectation |
|---|---|
| Browser shell | Launches into a familiar mobile browser layout. |
| Address bar | Accepts URLs and search queries; supports edit, submit, back, forward, and reload actions. |
| JavaScript | Enabled by default for modern websites. |
| Navigation | Back, forward, reload, and stop-loading controls update with page state. |
| Menu | The `⋮` control appears directly after the address bar. |
| Tabs | Create, switch, and close basic tabs. A private-tab entry point exists even if isolation is implemented later. |
| Permissions | Requests camera, microphone, location, notification, and download/storage access only when needed. |
| Downloads | Establishes the basic download path to Android Downloads. |
| Settings | Provides the navigation structure for appearance, privacy, site settings, downloads, language, search engine, profiles, network, performance, and About. Future sections are clearly marked as planned. |
| About | Shows app version, the actual integrated Chromium version, Phoenix attribution, copyright, licenses, and third-party notices. |
| Errors | Shows a friendly PHNX error page instead of crashing on network, URL, TLS, timeout, renderer, or download failures. |
| Branding | Uses the PHNX Browser name, launcher icon, Phoenix attribution, and version information. |
| CI | Runs tests and builds debug/release APK plus release AAB artifacts. |

## Suggested project structure

```text
phnx-browser/
├── .github/
│   └── workflows/
│       └── android.yml
├── app/
│   └── src/main/
│       ├── java/com/phoenix/phnx/
│       │   ├── MainActivity.kt
│       │   ├── browser/
│       │   │   ├── BrowserController.kt
│       │   │   ├── BrowserView.kt
│       │   │   └── NavigationController.kt
│       │   ├── tabs/
│       │   │   ├── Tab.kt
│       │   │   └── TabManager.kt
│       │   ├── menu/
│       │   │   └── BrowserMenu.kt
│       │   ├── settings/
│       │   │   └── SettingsActivity.kt
│       │   └── about/
│       │       └── AboutActivity.kt
│       ├── res/
│       │   ├── layout/
│       │   ├── drawable/
│       │   ├── mipmap/
│       │   └── values/
│       │       ├── strings.xml
│       │       ├── colors.xml
│       │       └── themes.xml
│       └── AndroidManifest.xml
├── PHASES/
│   └── phase-01-foundation/
│       ├── README.md
│       ├── diagrams/
│       │   ├── architecture.mmd
│       │   ├── browser-screen.mmd
│       │   ├── ci-pipeline.mmd
│       │   ├── downloads.mmd
│       │   └── phase-roadmap.mmd
│       └── diagrams/rendered/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── README.md
```

The exact Chromium integration directory may be adjusted after the integration strategy is selected, but the browser, navigation, tabs, menu, settings, and About concerns should remain modular.

## Browser screen

The initial browser screen should feel like a normal mobile browser. The three-dot menu must sit immediately after the address bar.

```text
┌──────────────────────────────────────┐
│ ←   →   🔒 example.com          ⋮   │
├──────────────────────────────────────┤
│                                      │
│             WEB PAGE                 │
│                                      │
├──────────────────────────────────────┤
│   +   │   Tab count   │     ⋮       │
└──────────────────────────────────────┘
```

The detailed layout is intentionally open to refinement, but the address bar, navigation controls, page area, tab affordance, and menu placement are not optional.

## Address bar and navigation behavior

The address bar accepts both URLs and search queries. Inputs beginning with `http://` or `https://` navigate directly. Other text is submitted to the configured search engine. The browser must support editing the current URL, submitting a new destination, going back, going forward, reloading, and stopping a page while it loads.

The UI should reflect page state: show a loading indicator during navigation, show the page after a successful load, and show a friendly error screen with a Retry action when navigation fails.

## Browser menu

The Phase 1 menu contains the following entries:

```text
New tab
New private tab
Bookmarks
History
Downloads
Share
Find in page
Desktop site
Add to Home screen
Settings
About PHNX
```

Items that are not yet implemented may be disabled or display a clear placeholder. The application must not claim that an unavailable feature works.

## Tabs

The `Tab` model should contain an ID, title, URL, favicon reference, and browser-state reference. `TabManager` should expose `createTab()`, `closeTab()`, `switchTab()`, `currentTab()`, and `getTabs()`. Profile functionality must not be placed into `TabManager` during Phase 1.

## Permissions, downloads, settings, and legal placeholders

Normal Android/browser permissions must be requested when websites need camera, microphone, location, notifications, or download access. Permissions must not be granted indiscriminately.

The basic download flow is:

```text
Website → download request → PHNX download manager → Android Downloads
```

Settings should include navigation placeholders for future profile, network, performance, and privacy work. The About screen should display the actual integrated Chromium version generated from the build rather than a manually claimed value. Legal navigation should provide placeholders for Privacy Policy, Terms of Service, Open Source Licenses, and Third-Party Notices without inventing privacy claims before the real data behavior is known.

## GitHub Actions and artifacts

Create `.github/workflows/android.yml` with the following flow:

```text
Git push
  ↓
Checkout
  ↓
Install JDK and Android SDK
  ↓
Configure Gradle and restore cache
  ↓
Run tests
  ↓
Build debug APK, release APK, and release AAB
  ↓
Upload workflow artifacts
```

At minimum, the workflow must run:

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
```

Expected artifact names are `PHNX-debug.apk`, `PHNX-release.apk`, and `PHNX-release.aab`. Release signing may be added later with GitHub Secrets. A keystore or signing password must never be committed to GitHub.

## Acceptance checklist

### Application

- [ ] PHNX installs on Android and launches without crashing.
- [ ] Webpages load over HTTP and HTTPS, with JavaScript enabled.
- [ ] Address bar supports URLs, search, editing, and submission.
- [ ] Back, forward, reload, and stop-loading work.
- [ ] Tabs can be created, switched, and closed.
- [ ] Private-tab entry point exists.
- [ ] The `⋮` button is directly beside the address bar and opens the menu.
- [ ] Settings and About PHNX open correctly.
- [ ] Basic downloads work.
- [ ] Navigation and renderer failures show a friendly error page rather than crashing.

### Branding

- [ ] PHNX Browser name appears correctly.
- [ ] PHNX launcher icon exists.
- [ ] Phoenix builder attribution appears.
- [ ] Version information is displayed.
- [ ] Actual Chromium version is surfaced from the build.

### Build and delivery

- [ ] GitHub Actions runs successfully.
- [ ] JDK and Android SDK setup succeeds.
- [ ] Gradle tests and builds succeed.
- [ ] APK and AAB artifacts are generated and uploaded.

## Critical implementation rule

> Do not implement Phase 2–9 features prematurely. Complete and test Phase 1 first. Do not replace Chromium with a different browser engine, WebView-only implementation, Flutter, React Native, or a web wrapper. Keep the architecture modular so profile, network, resource-management, privacy, and Chromium-integration layers can expand later. Do not claim functionality that has not been implemented and tested.

## Related diagrams

The diagram source files are stored in [`diagrams/`](./diagrams/), with rendered PNGs in [`diagrams/rendered/`](./diagrams/rendered/). The source files are intentionally kept beside their rendered outputs so future implementation changes can update both consistently.

- [Architecture](./diagrams/rendered/architecture.png)
- [Browser screen](./diagrams/rendered/browser-screen.png)
- [CI pipeline](./diagrams/rendered/ci-pipeline.png)
- [Download flow](./diagrams/rendered/downloads.png)
- [Nine-phase roadmap](./diagrams/rendered/phase-roadmap.png)
