# PHNX Phase Audit

Audit date: 2026-09-07

Audit basis: phase blueprints in `PHASES/phase-01-foundation`, repository code, unit tests, GitHub Actions, release workflows, and the live Pages site. This document distinguishes shipped behavior, platform limits, and work that still requires a signed public release or device validation.

## Verified Complete

- Phases 1-7 have working prototypes for the browser shell, profiles, provider-agnostic network settings, identity presets, lifecycle policy, privacy settings, tabs, history, bookmarks, downloads, search routing, and page controls.
- Tab groups, bookmark folders, history search/date grouping, start-page bookmark/history content, full-screen WebView handling, renderer retry recovery, first-party cookie control, native zoom/text selection, find-in-page navigation/count, profile-aware shortcuts, and profile/site page controls are implemented.
- HTTPS/HTTP/certificate/Safe Browsing state indicators, download URL/type safety checks, dangerous-download confirmation, mixed-content blocking, and persisted crash-loop recovery state are implemented.
- Profile scheduling exposes explicit freeze, suspend, and close selection with lifecycle-aware eligibility and unit coverage. Main browser reconciliation applies lifecycle decisions to the profile view pool.
- The `legal/` package documents current privacy behavior, terms, dependency license families, and third-party services.
- Phase 10 has profile-scoped baseline ad/tracker request interception, ABP/hosts-style custom ruleset parsing with checksum and signed-artifact validation primitives, site exceptions, and persistent request statistics.
- Phase 11 has typed manifest validation, numeric version comparison, SHA-256 artifact verification, ECDSA manifest/artifact signature verification, a guarded update state machine, atomic staged artifact storage with rollback, and a FileProvider-based install handoff.
- The tag-based release workflow verifies release secrets, Android APK/AAB signing, the separate update signing key, manifest signatures, checksums, and temporary-file cleanup. It intentionally does not create a release until a version-matched tag is supplied.
- Phase 12 registers safe HTTP/HTTPS browser intents, validates external URLs, exposes Android browser-role status/requesting, provides system settings links, routes user-gesture pop-ups into tabs, supports file selection, and honors profile IDs on shortcut intents.
- Network settings now have an explicit save/apply action for Direct, My Proxy, and Free Public Proxy modes. Saved configuration remains profile-scoped.
- Google Services Gradle configuration is present for the release package and the `.debug` application variant.
- The website has product, Features, Download, Releases, Changelog, Documentation, Privacy, Security, About, and GitHub sections. It generates route-relative links/assets, `sitemap.xml`, and `robots.txt`, and the live root URL is `https://phoenix1185.github.io/phnx-browser/`.
- The Pages workflow runs on `main`, validates the generated routes and security markers, uploads the `website/` artifact, and deploys successfully. It is not gated by a `PAGES_ENABLED` flag.
- Resource shrinking is enabled and the latest Android workflow passed lint, unit tests, instrumentation-test compilation, debug/release APK builds, and release AAB builds.

## Platform Limits

- Phase 1 uses Android System WebView rather than an independently packaged Chromium engine. Hosted emulator execution is not a reliable substitute for a device smoke test.
- Phase 2 private tabs provide session/history hygiene but do not have a separate WebView cookie/storage partition. Profile switching is process-restart based rather than transactional.
- Phase 3 WebView proxy override is process-wide. Android WebView does not expose enforceable DNS or WebRTC routing controls, so proxy mode is best-effort rather than leak-proof.
- Phase 4 Android WebView does not expose reliable custom screen metrics, locale, timezone, or full client-hint control. PHNX reports those limits rather than injecting page scripts.
- Phase 6 full tracker blocking is not exposed by Android WebView. Do Not Track is stored but cannot be emitted through a supported WebView API. WebView global data APIs still require device-level validation for every profile boundary.
- Phase 7 Android DownloadManager exposes status and progress but does not provide PHNX-owned pause/resume semantics. A full custom downloader would be a separate product decision.

## Remaining Engineering Work

- Phase 5/8: Room managers still permit synchronous main-thread queries. Startup profiling, lazy initialization, bounded stress/long-run tests, and production crash reporting are not complete.
- Phase 8: broader low-resource/high-resource device validation, battery/thermal profiling, and long-run WebView resource tests remain.
- Phase 9: no stable public release has been published. Upgrade/migration coverage, exact generated license notices, network-security hardening, repository branch/security controls, rollback documentation, and device-level end-to-end release validation remain.
- Phase 10: remote maintained-list scheduling, signed ruleset manifests, richer resource-type/context matching, malicious-ad coverage, and ruleset rollback remain.
- Phase 11: the app remains discovery-only. Background download policy, full APK download fallback, delta application, atomic package apply, startup rollback, staged rollout, key rotation, and mandatory-security-update policy remain.
- Phase 12: external-intent profile selection, complete Android permission-center management, capture-mode policy, and a device validation matrix remain.
- Phase 13: custom-domain migration is not configured. The current Pages site is complete for the repository domain; published release metadata remains empty until a signed GitHub Release exists.

## Policy Notes

- Public proxy routes are third-party routes that may inspect or modify traffic. PHNX does not silently bypass configured routes or claim anonymity.
- Unsupported identity fields, WebView privacy controls, proxy routing controls, and private-storage boundaries are disclosed in the app and website instead of being presented as guarantees.
- Android notification permission is requested only for PHNX app features such as download notifications. Website notifications are not exposed by Android System WebView and are not silently emulated.
- CI artifacts, debug builds, unsigned packages, and workflow links are never presented as public product downloads.
