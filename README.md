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
- Profile metadata, profile-owned tab sessions, and Android WebView data-directory separation.
- Profile-scoped network configuration persistence, secure proxy credentials, connection testing, and connectivity monitoring.
- Profile-scoped identity presets, consistency validation, persistence, and a WebView capability-reporting editor.
- Resource-pressure policy, profile-priority scheduling, and Android memory/battery/thermal snapshot foundations; lifecycle adapters and CPU monitoring remain in progress.

The browser engine is isolated behind `BrowserController` and `BrowserView`; the current adapter uses the Android system's Chromium-backed WebView runtime. A separately embeddable Chromium distribution is not committed to this repository. Android WebView does not expose the per-profile proxy routing required by the Phase 3 blueprint, so proxy configuration is persisted, testable, and reported as unsupported at application time rather than silently claimed as applied. On-device browser-context isolation, profile-specific permissions/history/bookmarks, and the later blueprint phases still require further implementation and testing.

## Development rule

Complete and test Phase 1 before implementing features assigned to Phases 2–9. Do not claim functionality that has not been implemented and tested.
