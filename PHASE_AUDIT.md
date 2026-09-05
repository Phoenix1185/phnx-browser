# PHNX Phase Audit

Audit basis: phase blueprints in `PHASES/phase-01-foundation`, repository code, tests, and the latest GitHub Actions build.

## Verified

- Unit tests and debug/release APK plus AAB builds pass in GitHub Actions.
- Phases 1-7 have working prototypes for the browser shell, profiles, network settings, identity presets, lifecycle policy, privacy settings, tabs, history, bookmarks, downloads, search routing, and page controls.
- Recent work added tab groups, bookmark folders, history search/date grouping, start-page bookmark/history content, full-screen WebView handling, renderer retry recovery, first-party cookie control, and native zoom/text selection.
- Release R8/resource shrinking is enabled and verified by the latest debug/release APK and AAB build.

## Remaining Gaps

- Phase 1: no instrumentation suite; the app still uses Android System WebView rather than an independently packaged Chromium engine.
- Phase 2: each tab now retains its own WebView and navigation state; private tabs do not have a separate cookie/storage partition; profile switching is process-restart based and not transactional.
- Phase 3: WebView proxy override is process-wide; Android WebView does not expose enforceable DNS or WebRTC routing controls, so proxy mode is documented as best-effort rather than leak-proof.
- Phase 4: custom identity fields and real locale/timezone/client-hint control are unsupported. The app now reports those limits instead of injecting fingerprint overrides.
- Phase 5: lifecycle/resource policy is prototype-level; crash recovery, crash-loop protection, and persisted runtime recovery state are missing.
- Phase 6: actual tracking protection, browser security-state indicators, download security, and fully profile-scoped clear-data coverage remain incomplete.
- Phase 7: download pause/resume, find-next/previous/count, profile/site-persisted page controls, profile-aware shortcuts, and full settings organization remain incomplete; download progress and sharing are implemented.
- Phase 8: Room managers use main-thread queries; lazy startup, performance snapshots/configuration, startup profiling, bounded stress tests, database optimization, and crash recovery are not production-ready.
- Phase 9: release signing, tagged release workflow, migration/instrumentation upgrade tests, network-security hardening, legal package generation, rollback documentation, and repository security controls remain.

## Policy Notes

- Google or public proxies can close automated/WebView connections. PHNX does not silently bypass configured routes; connection errors expose Network Settings and an alternate search option.
- Android WebView cannot change the physical device screen resolution per profile. Supported User-Agent and viewport settings are applied; unsupported screen, locale, timezone, and client-hint changes are reported honestly.
- Private browsing is currently local history/session hygiene, not a claim of complete cookie/storage isolation.
- Android notification permission is requested for PHNX app downloads. Website notifications are not exposed by Android System WebView and are not silently emulated or granted.
