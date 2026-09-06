# PHNX Browser — Phase 9 Blueprint

## Release, CI/CD, and Production Hardening

> **Scope:** Prepare the existing PHNX Browser architecture from Phases 1–8 for production builds, secure signing, GitHub Actions CI/CD, automated testing, release management, hardening, internal/beta testing, safe rollback, and hotfix releases.

> **Preservation rule:** Do not rebuild the application, replace Chromium, migrate frameworks, or remove existing systems. Phase 9 hardens and packages the architecture already established.

## Production architecture

The production system preserves the Chromium engine, native Android/Kotlin UI, profile isolation, per-profile network and device configuration, lifecycle/resource management, privacy/security, browser features, diagnostics, and performance systems. Release infrastructure adds Gradle production builds, R8, signing, GitHub Actions, testing, artifacts, and release management.

```text
PHNX Browser
├── Chromium Browser Engine
├── Browser UI
├── Profiles
│   ├── Persistent isolation
│   ├── Network configuration
│   ├── Device/browser configuration
│   └── Lifecycle management
├── Privacy & Security
├── Tabs, History, Bookmarks, Downloads, Search
├── ResourceManager and PerformanceManager
├── Diagnostics
└── Release Infrastructure
    ├── Gradle
    ├── R8
    ├── Secure signing
    ├── GitHub Actions
    ├── Testing
    ├── Artifacts
    └── Release management
```

## Production build configuration

Configure `debug` and `release` build types. Debug builds may enable debugging and development diagnostics for local testing but must never be distributed as public production builds. Release builds disable debugging, enable compatible R8/minification and resource shrinking, remove development-only behavior, use release signing, and produce production APK/AAB artifacts.

Define `versionCode` and `versionName`. Use `MAJOR.MINOR.PATCH` versioning such as `1.0.0`, `1.0.1`, `1.1.0`, and `2.0.0`. Every published release increments `versionCode`; never reuse a published code.

Build metadata should include application name, version, build number, actual Chromium version, and build type. The About page must obtain Chromium version from the actual embedded/runtime implementation rather than a hardcoded value.

## Secure release signing

Configure Android release signing with a keystore, alias, and passwords held outside the repository. Never commit keystores, `.jks`, `.p12`, passwords, signing credentials, API keys, or private keys. Use GitHub Actions Secrets, for example:

```text
ANDROID_KEYSTORE_BASE64
ANDROID_KEYSTORE_PASSWORD
ANDROID_KEY_ALIAS
ANDROID_KEY_PASSWORD
```

The release workflow should create a temporary keystore, sign APK/AAB, upload artifacts, and delete temporary signing files without printing secrets or exposing password variables in logs.

## GitHub Actions CI/CD

Create:

```text
.github/workflows/
├── ci.yml
├── release.yml
└── dependency-check.yml
```

`ci.yml` runs on push and pull request. It checks out the repository, pins a compatible JDK and Android SDK, restores Gradle cache, validates Gradle, runs lint, executes unit and instrumentation tests, builds debug APK, builds release APK/AAB, and uploads artifacts. Any failed test fails the workflow. Use the committed Gradle wrapper rather than a globally installed Gradle version.

At minimum, validate:

```bash
./gradlew tasks
./gradlew lint
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew bundleRelease
```

Pin important action versions and avoid unnecessary floating dependencies.

## Automated testing and isolation

Production CI must test the managers and integrations from Phases 1–8, including profiles, networking, identity validation, lifecycle/resource management, privacy/security, browser features, performance, and crash recovery.

Profile isolation tests verify that Profile A cannot read Profile B's cookies, LocalStorage, IndexedDB, cache, history, bookmarks, permissions, session, network configuration, or identity configuration. Deleting Profile A must not affect Profile B.

Also test Direct/Proxy/invalid-proxy/network-loss/network-recovery flows, lifecycle transitions and restoration, private browsing, clear-data behavior, cookie policy, tracking protection, JavaScript defaults, HTTPS validation, permission protection, and absence of sensitive credentials from logs, analytics, diagnostics, artifacts, and inappropriate database records.

## R8, native compatibility, and ABI

Enable R8 and resource shrinking where compatible. Maintain focused `proguard-rules.pro` keep rules only where required; never use blanket rules such as `-keep class ** { *; }`. After optimization, test launch, Chromium startup, profile creation/switching, tabs, private tabs, network, downloads, bookmarks, history, permissions, settings, lifecycle, diagnostics, and crash recovery.

Verify JNI, native libraries, Chromium components, reflection, services, and WebView/Chromium integration where applicable. Do not remove runtime-required native components. Evaluate `arm64-v8a` and every additional ABI actually supported by the Chromium build; do not advertise unsupported ABIs.

## Android and security audits

Audit `AndroidManifest.xml` and remove unnecessary permissions, activities, services, receivers, providers, intent filters, and exported components. Configure `android:exported` correctly and require a legitimate reason for exposure.

Request only permissions required by implemented functionality, using Android runtime permission mechanisms for camera, microphone, location, and notifications where required. Add production network security configuration where appropriate: HTTPS works normally, cleartext traffic is disabled unless specifically required, certificate validation remains enabled, debug trust configuration does not leak into release, no development proxy is hardcoded, and no development certificate authority ships in production.

Review sensitive storage including proxy credentials, encryption keys, tokens, signing material, and session secrets. Use Android Keystore-backed protection where appropriate; do not store secrets in SQLite, SharedPreferences, logs, JSON files, or Git unless genuinely non-sensitive.

Review Android backup rules for profile data, cookies, sessions, permissions, network credentials, history, bookmarks, and download metadata. Exclude data that cannot be backed up safely and never expose credentials through backup.

## Diagnostics and crash recovery

Production crash handling preserves user data. On restart, recover profile metadata and tabs/session where possible, then restore a safe state. Repeated profile failures mark the profile recovery-required, stop infinite restart loops, offer recovery options, and keep persistent data.

Diagnostics may contain app and Chromium versions, Android version, device class, resource states, lifecycle state, tab count, and crash/recovery state. They must not contain passwords, proxy passwords, private keys, authentication tokens, page contents, private messages, or sensitive browsing content.

If diagnostic export is offered under `Settings → Advanced → Diagnostics → Export diagnostics`, sanitize the output first.

## Dependencies and reproducibility

Review Gradle, Android Gradle Plugin, Kotlin, AndroidX, Chromium, and third-party libraries. Remove unused or abandoned dependencies where practical. Use Gradle dependency locking where appropriate so the same source and versions produce predictable builds. `dependency-check.yml` identifies outdated, vulnerable, and incompatible dependencies; updates still require review and testing.

## Resource and startup hardening

Verify resource shrinking without removing assets required dynamically by Chromium/native components. If supported, use Baseline Profiles for startup, MainActivity, browser startup, address bar, tab creation, profile switching, and settings, measuring before and after.

Production startup performs minimal initialization, loads required UI and Chromium, restores only the active profile/tab, and initializes non-critical services lazily. Startup remains bounded for 10, 50, and 100+ saved profiles by loading metadata first and runtime only when needed. Do not initialize every profile, Chromium context, or tab at launch.

## Branding and legal readiness

Finalize consistent PHNX branding across app name, launcher label/icon, adaptive icon, splash screen, About page, settings, notifications, share targets, and error pages. The production About page includes PHNX Browser, version/build, actual Chromium version, Phoenix attribution, copyright, Open Source Licenses, Third-Party Notices, Privacy Policy, and Terms of Service.

Maintain the following legal package from actual product behavior:

```text
legal/
├── privacy_policy
├── terms_of_service
├── open_source_licenses
└── third_party_notices
```

Do not claim complete privacy, anonymity, undetectability, zero data collection, or Play Store approval without evidence. Generate open-source notices from the actual shipped dependency set where practical.

## Official app icon

The supplied PHNX Browser artwork is the official app-icon reference for branding and release work. The source asset is stored at [`assets/branding/app-icon/phnx-browser-app-icon.png`](../../../assets/branding/app-icon/phnx-browser-app-icon.png).

Use this artwork as the visual source when preparing Android adaptive-icon foreground/background layers and launcher density variants. Preserve the phoenix, globe, dark rounded-square treatment, orange/yellow highlights, and PHNX Browser wordmark. Any production crop or adaptive-icon safe-zone adjustment must retain recognizable branding and should be validated across light/dark launchers, masks, and Android densities.

![Official PHNX Browser app icon](../../../assets/branding/app-icon/phnx-browser-app-icon.png)

## Release artifacts and workflow

Every successful release produces clearly versioned APK and AAB artifacts, for example:

```text
phnx-browser-1.0.0-release.apk
phnx-browser-1.0.0-release.aab
```

The preferred release trigger is a version tag such as `v1.0.0`, `v1.0.1`, or `v1.1.0`. Before creating a GitHub Release, tests, lint, build, signing, and artifact verification must succeed. Verify artifact existence, version code/name, application ID, release type, signature, expected ABI, and expected permissions.

Release sequence:

```text
Merge tested code → update version → create tag → push tag
→ run CI → build signed APK/AAB → generate metadata
→ create GitHub Release → attach artifacts
```

## Internal testing, upgrades, hotfixes, and rollback

Test real devices through developer, internal, closed/beta, and production stages across Android versions, RAM classes, screen sizes, CPU classes, thermal conditions, and network types. The production matrix includes install, upgrade, uninstall/reinstall, first launch, browser functions, profiles, networking, lifecycle, privacy/security, downloads, accessibility, low RAM, battery saver, thermal pressure, and crash recovery.

Upgrade testing verifies that profiles, tabs/session, bookmarks, history, settings, permissions, and download metadata survive version changes. Every schema change has a migration; never solve incompatibility by deleting the database. Migration failure protects existing data, reports the failure, and offers recovery.

For critical production bugs, create a focused hotfix branch, make the smallest change, run regression tests, increment the PATCH version, build, sign, test, and release. Keep previous artifacts and never overwrite historical releases. Configure branch protection so `main` requires pull requests, passing CI, appropriate review, no committed secrets, and no direct unsigned production changes. Enable secret scanning and dependency security features where available.

## Repository and logging audits

Search the repository and build outputs for `API_KEY`, `SECRET`, `PASSWORD`, `TOKEN`, `PRIVATE_KEY`, and `BEGIN PRIVATE KEY`. Inspect `.env`, `local.properties`, keystores, debug configuration, test credentials, and generated artifacts. If a real secret was committed, rotate or revoke it; deleting it only from the latest commit is insufficient.

Release logging is controlled and privacy-conscious. Avoid noisy or sensitive `println`, debug, and verbose logs. Never log inappropriate URLs, cookies, tokens, passwords, proxy credentials, or private browsing content.

## Performance regression gates

Track startup time, profile-switch time, tab-creation time, memory usage, crash rate, and ANR rate where practical. Do not reject builds based on arbitrary numbers without validating device and test context.

## Final end-to-end test

Install PHNX, create and configure Profile A, browse, create and configure Profile B, browse independently, switch between profiles, open tabs, download, bookmark, inspect history, apply privacy settings, background and resume a profile, restart the app, restore sessions, upgrade the app, and verify all persistent data and feature integrations remain correct.

## Acceptance checklist

- [ ] Release Gradle configuration and version management work.
- [ ] Release signing uses protected GitHub Secrets.
- [ ] CI runs automatically on pushes and pull requests.
- [ ] Unit tests, instrumentation tests, and lint pass.
- [ ] R8, resource shrinking, native components, and ABIs work in release builds.
- [ ] APK/AAB build and signing verification succeed.
- [ ] Phases 1–8 remain intact after optimization and packaging.
- [ ] Profile, network, identity, lifecycle, privacy, browser-feature, and performance tests pass.
- [ ] Manifest, permissions, secure storage, backup rules, and dependencies are audited.
- [ ] Diagnostics and logs exclude sensitive information.
- [ ] Legal documents and open-source notices reflect actual shipped behavior.
- [ ] Branding, icons, splash, and About page are finalized.
- [ ] Upgrade, migration, internal, stress, and end-to-end tests pass.
- [ ] Hotfix and rollback processes are documented.
- [ ] Branch protection and repository security controls are enabled.
- [ ] Release workflow creates versioned signed APK/AAB artifacts.

## Explicit exclusions

Do not claim Play Store approval, public release readiness, complete anonymity, guaranteed undetectability, or unsupported Chromium functionality. Do not commit release credentials or secrets. Do not rewrite the application, replace Chromium, migrate frameworks, disable prior managers, silently weaken certificate validation, silently grant permissions, delete profiles because of crashes, or introduce anti-bot, anti-fraud, CAPTCHA, or access-control bypass mechanisms.

## Final PHNX state

After Phase 9, PHNX Browser is a native Kotlin/Android Chromium-based browser with isolated profiles, per-profile network and device configuration, lifecycle/resource management, privacy/security, complete browser features, performance optimization, crash recovery, diagnostics, production Gradle/R8, secure signing, GitHub Actions, APK/AAB artifacts, release management, and accurate legal/open-source notices.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Production architecture](./diagrams/rendered/production-architecture.png)
- [CI/CD release pipeline](./diagrams/rendered/cicd-release-pipeline.png)
- [Secure signing flow](./diagrams/rendered/secure-signing-flow.png)
- [Release and rollback flow](./diagrams/rendered/release-rollback.png)
- [Nine-phase validation](./diagrams/rendered/nine-phase-validation.png)
