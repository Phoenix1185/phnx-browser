# PHNX Browser

A Chromium-based Android browser built and engineered by Phoenix.

## Project documentation

The current implementation scope is documented in [Phase 1 — Foundation + Chromium Browser Shell](./PHASES/phase-01-foundation/README.md).

The implementation gap inventory is tracked in [PHASE_AUDIT.md](./PHASE_AUDIT.md).

That folder also contains the editable Mermaid diagram sources and rendered PNG diagrams used to communicate the architecture, browser layout, download flow, CI pipeline, and nine-phase roadmap.

## Current implementation

Phase 1 now has a buildable native Kotlin Android shell in `app/`. It includes:

- Chromium-backed Android browsing with JavaScript and DOM storage enabled.
- URL/search address-bar routing, back, forward, reload, loading/stop state, and friendly navigation errors.
- Basic tabs with switching and closing, profile-scoped tab groups, plus a private-tab entry point.
- The three-dot browser menu, About screen, branding, sharing, recent-tab restore, basic find-in-page, page zoom, text size, desktop-site toggle, pinned home-screen shortcuts, profile-scoped download records/statuses, Android DownloadManager integration, and website full-screen handling.
- Unit tests for navigation routing and tab management.
- GitHub Actions builds for tests, debug/release APKs, and the release AAB.
- Profile metadata, profile-owned tab sessions, and a disposable per-profile WebView pool. Profile switching preserves Android WebView data-directory isolation by restarting the process.
- Profile-scoped network configuration persistence, secure proxy credentials, connection testing, health monitoring, and connectivity monitoring.
- Explicit Direct, My Proxy, and Free Public Proxy modes. My Proxy accepts a generic user-supplied scheme, host, port, optional username, and optional password without provider-specific defaults or branches. Free proxy discovery uses public feeds, caches results for five minutes, checks connectivity and latency with bounded parallelism, supports protocol/country/HTTPS/health filters, and provides explicit route selection or auto-selection. Public feeds are volatile and untrusted; do not use them for passwords, payments, private API requests, or authenticated sessions.
- Profile-scoped identity presets, consistency validation, persistence, and a WebView capability-reporting editor with 20 selectable device and browser profiles plus System Default. Unsupported screen-metric, locale, timezone, and client-hint overrides are reported instead of being spoofed with page scripts.
- New profiles receive a rotating selectable identity preset; Reset returns to the actual System Default identity rather than reapplying the same hard-coded phone preset.
- Resource-pressure policy, profile-priority scheduling, Android memory/battery/thermal snapshots, process CPU sampling from `/proc`, and a prototype per-profile view lifecycle (`ACTIVE` → `IDLE` → `FROZEN` → `SUSPENDED` → `RECREATING` → `ACTIVE`).
- Phase 6 privacy settings persisted per profile for JavaScript, first-party and third-party cookies, pop-ups, Safe Browsing, and stored tracking preferences; unsupported Do Not Track, full tracker blocking, and website notifications are reported honestly. Android notification permission is requested for PHNX app features such as downloads.
- Profile/origin site permission decisions for camera, microphone, and location are persisted separately from Android OS permissions and can be reviewed or reset in Settings.
- Active-profile clear-data controls remove selected WebView cookies, site storage, cache, and in-memory navigation data without deleting profile configuration; history and permission cleanup remain separate controls.
- Profile-aware history and bookmarks are persisted, history supports search/date grouping and retention cleanup, bookmark folders support create/rename/delete and moving bookmarks, private visits are excluded from history, private tabs are not restored into saved sessions, and both are available from the browser menu.
- History retention is selectable: remember until manually cleared, or clear when PHNX closes its task.
- Address-bar searches default to Google and can be switched to Bing, DuckDuckGo, Brave Search, Startpage, Ecosia, or Yahoo while direct URL detection remains unchanged.
- Phase 10 blocker controls include baseline domain decisions, ABP/hosts-style checksum-validated custom rulesets, profile exceptions, and persistent blocked-request statistics.
- Phase 11 includes typed update manifests, SHA-256 and ECDSA verification primitives, and guarded update-state transitions. Installation remains discovery-only until release infrastructure exists.
- Phase 12 includes safe browser intents, Android browser-role controls, user-gesture pop-ups routed into tabs, file selection, and profile-aware home-screen shortcut routing.
- Phase 13 includes the live structured website at `https://phoenix1185.github.io/phnx-browser/` with product, release, documentation, privacy, security, and About sections.

The browser engine is isolated behind `BrowserController`, `ProfileViewPool`, and `BrowserView`; the current adapter uses the Android system's Chromium-backed WebView runtime. A separately embeddable Chromium distribution is not committed to this repository. AndroidX WebKit proxy override applies to all WebViews in the app process, so PHNX applies the active profile's route and restarts the process when switching profile data directories rather than claiming simultaneous per-profile proxy partitions. Public proxies are untrusted and must not be used for credentials, payments, or private data. PHNX never silently falls back to a direct route; direct fallback is an explicit profile setting. Android WebView does not expose enforceable DNS or WebRTC proxy routing controls, so proxy mode is not advertised as leak-proof. Profile session metadata survives view destruction and recreation; private tabs currently provide session/history hygiene but not a separate WebView cookie partition. See `PHASE_AUDIT.md` for the remaining release-infrastructure and Android-platform gaps.

## Development rule

Complete and test Phase 1 before implementing features assigned to Phases 2–9. Do not claim functionality that has not been implemented and tested.
