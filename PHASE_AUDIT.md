# PHNX Phase Audit

Audit basis: phase blueprints in `PHASES/phase-01-foundation`, repository code, tests, and the latest GitHub Actions build.

## Verified

- Unit tests, instrumentation-test compilation, and debug/release APK plus AAB builds pass in GitHub Actions.
- Phases 1-7 have working prototypes for the browser shell, profiles, network settings, identity presets, lifecycle policy, privacy settings, tabs, history, bookmarks, downloads, search routing, and page controls.
- Recent work added tab groups, bookmark folders, history search/date grouping, start-page bookmark/history content, full-screen WebView handling, renderer retry recovery, first-party cookie control, native zoom/text selection, find-in-page navigation/count, profile-aware shortcuts, and profile/site page controls.
- Recent work also added basic HTTPS/HTTP/certificate/Safe Browsing state indicators, download URL/type safety checks, dangerous-download confirmation, mixed-content blocking, and persisted crash-loop recovery state.
- Profile scheduling now exposes explicit freeze, suspend, and close selection with lifecycle-aware eligibility and unit coverage.
- A reviewable `legal/` package now documents the current privacy behavior, terms, dependency license families, and third-party services.
- Phase 10 now has profile-scoped baseline ad/tracker request interception, ABP/hosts-style custom ruleset parsing with checksum and signed-artifact validation primitives, site exceptions, and persistent request statistics. It blocks intercepted WebView requests rather than claiming visual ad removal.
- Phase 11 now has typed manifest validation, numeric version comparison, SHA-256 artifact verification, ECDSA manifest/artifact signature verification, and an explicit guarded update state machine; the existing UI remains discovery-only.
- Phase 12 now registers safe HTTP/HTTPS browser intents, validates external URLs, exposes Android browser-role status/requesting, provides system settings links, routes user-gesture pop-ups into tabs, supports file selection, and honors profile IDs on shortcut intents.
- Phase 13 now has a responsive structured website with Features, Download, Releases, Changelog, Documentation, Privacy, Security, About, and GitHub sections. The legacy GitHub Pages URL now redirects into it and has been verified live at `https://phoenix1185.github.io/phnx-browser/`.
- Release R8/resource shrinking is enabled and verified by the latest debug/release APK and AAB build.

## Remaining Gaps

- Phase 1: an offline About-screen instrumentation smoke test is present and compiled in CI, but hosted emulator execution is not reliable in the current runner; the app still uses Android System WebView rather than an independently packaged Chromium engine.
- Phase 2: each tab now retains its own WebView and navigation state; private tabs do not have a separate cookie/storage partition; profile switching is process-restart based and not transactional.
- Phase 3: WebView proxy override is process-wide; Android WebView does not expose enforceable DNS or WebRTC routing controls, so proxy mode is documented as best-effort rather than leak-proof.
- Phase 4: custom identity fields and real locale/timezone/client-hint control are unsupported. The app now reports those limits instead of injecting fingerprint overrides.
- Phase 5: lifecycle/resource policy is prototype-level; crash-loop recovery is implemented, but broader resource policy and production crash reporting remain.
- Phase 6: actual tracking protection and fully profile-scoped clear-data coverage remain incomplete; security-state indicators and baseline download safety are implemented.
- Phase 7: download pause/resume and full settings organization remain incomplete; find-in-page controls, profile/site page controls, profile-aware shortcuts, progress, and sharing are implemented.
- Phase 8: Room managers still use main-thread-compatible synchronous APIs; performance diagnostics now move database and resource sampling off the UI thread, but lazy startup, startup profiling, bounded stress tests, and database optimization are not production-ready.
- Phase 9: protected signing secrets are not configured, so the normal CI build remains unsigned; the repository now has a tag-based signed-release workflow, but migration/instrumentation upgrade tests, network-security hardening, exact generated license notices, rollback documentation, and repository security controls remain.
- Phase 10: remote maintained-list scheduling, signed ruleset manifests, malicious-ad coverage, and richer resource-type/context matching remain.
- Phase 11: production key embedding/rotation, delta/full-APK download fallback, atomic apply, rollback, staged rollout, and background policy remain; the verifier and state machine are not wired to a release key, download service, or installer yet.
- Phase 12: browser-role, HTTP/HTTPS intent, user-gesture pop-up, and file-picker integration are implemented; external-profile routing, full Android permission-center controls, capture-mode policy, and device validation remain.
- Phase 13: the live legacy Pages source is verified, while the optional Actions deployment remains gated until the repository is switched to Pages-from-Actions and `PAGES_ENABLED=true`; generated release metadata and a fully separate documentation sitemap remain.

## Policy Notes

- Google or public proxies can close automated/WebView connections. PHNX does not silently bypass configured routes; connection errors expose Network Settings and an alternate search option.
- Android WebView cannot change the physical device screen resolution per profile. Supported User-Agent and viewport settings are applied; unsupported screen, locale, timezone, and client-hint changes are reported honestly.
- Private browsing is currently local history/session hygiene, not a claim of complete cookie/storage isolation.
- Android notification permission is requested for PHNX app downloads. Website notifications are not exposed by Android System WebView and are not silently emulated or granted.
