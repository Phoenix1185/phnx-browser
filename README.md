# PHNX Browser

A Chromium-based Android browser built and engineered by Phoenix.

## Project documentation

The current implementation scope is documented in [Phase 1 — Foundation + Chromium Browser Shell](./PHASES/phase-01-foundation/README.md).

That folder also contains the editable Mermaid diagram sources and rendered PNG diagrams used to communicate the architecture, browser layout, download flow, CI pipeline, and nine-phase roadmap.

## Current implementation

Phase 1 now has a buildable native Kotlin Android shell in `app/`. It includes:

- Chromium-backed Android browsing with JavaScript and DOM storage enabled.
- URL/search address-bar routing, back, forward, reload, stop/loading state, and friendly navigation errors.
- Basic tabs with switching and closing, plus a private-tab entry point.
- The required three-dot browser menu, settings placeholders, About screen, branding, sharing, find-in-page, desktop-site toggle, and Android Downloads integration.
- Unit tests for navigation routing and tab management.
- GitHub Actions builds for tests, debug/release APKs, and the release AAB.
- Profile metadata, profile-owned tab sessions, and a disposable per-profile WebView pool. Profile switching preserves Android WebView data-directory isolation by restarting the process.
- Profile-scoped network configuration persistence, secure proxy credentials, connection testing, and connectivity monitoring.
- Profile-scoped identity presets, consistency validation, persistence, and a WebView capability-reporting editor.
- Resource-pressure policy, profile-priority scheduling, Android memory/battery/thermal snapshots, and a real per-profile view lifecycle (`ACTIVE` → `IDLE` → `FROZEN` → `SUSPENDED` → `RECREATING` → `ACTIVE`). CPU monitoring remains unsupported by the current Android integration.
- Phase 6 privacy settings persisted per profile for JavaScript, third-party cookies, pop-ups, Safe Browsing, and stored tracking preferences; unsupported Do Not Track and full tracker blocking are reported honestly.
- Profile/origin site permission decisions for camera, microphone, and location are persisted separately from Android OS permissions and can be reviewed or reset in Settings.
- Active-profile clear-data controls remove selected WebView cookies, site storage, cache, and in-memory navigation data without deleting profile configuration.
- Profile-aware history and bookmarks are persisted, private visits are excluded from history, and both are available from the browser menu.

The browser engine is isolated behind `BrowserController`, `ProfileViewPool`, and `BrowserView`; the current adapter uses the Android system's Chromium-backed WebView runtime. A separately embeddable Chromium distribution is not committed to this repository. Android WebView does not expose the per-profile proxy routing required by the Phase 3 blueprint, so proxy configuration is persisted, testable, and reported as unsupported at application time rather than silently claimed as applied. Profile session metadata survives view destruction and recreation; site permissions, clear-data operations, private storage, history/bookmarks, and later blueprint phases still require further implementation and testing.

## Development rule

Complete and test Phase 1 before implementing features assigned to Phases 2–9. Do not claim functionality that has not been implemented and tested.
