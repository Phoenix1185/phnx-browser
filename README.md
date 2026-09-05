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

The browser engine is isolated behind `BrowserController` and `BrowserView`; the current adapter uses the Android system's Chromium-backed WebView runtime. A separately embeddable Chromium distribution is not committed to this repository. Profile isolation, network configuration, identity configuration, lifecycle management, privacy controls, and the remaining blueprint phases are not claimed as implemented.

## Development rule

Complete and test Phase 1 before implementing features assigned to Phases 2–9. Do not claim functionality that has not been implemented and tested.
