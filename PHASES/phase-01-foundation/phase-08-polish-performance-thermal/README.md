# PHNX Browser — Phase 8 Blueprint

## Polish, Performance, and Thermal Optimization

> **Scope:** Optimize PHNX Browser for real Android devices so it feels fast, smooth, stable, lightweight, RAM-efficient, CPU-efficient, battery-aware, and thermally responsible while multiple profiles exist.

> **Correctness rule:** Never sacrifice browser correctness, profile isolation, security, or user-visible functionality merely to reduce resource use. PHNX cooperates with Android lifecycle and resource rules and does not claim to prevent Android from killing processes.

## Objective

Measure and improve RAM, CPU, GPU, battery, thermal behavior, startup time, tab switching, profile switching, browser rendering, database performance, network efficiency, UI smoothness, crash recovery, and background resource use.

## Performance architecture

Suggested package structure:

```text
app/src/main/java/com/phoenix/phnx/
├── performance/
│   ├── PerformanceManager.kt
│   ├── PerformanceMonitor.kt
│   ├── StartupProfiler.kt
│   ├── UiPerformanceMonitor.kt
│   ├── MemoryOptimizer.kt
│   ├── CpuOptimizer.kt
│   ├── BatteryOptimizer.kt
│   ├── ThermalOptimizer.kt
│   ├── DatabaseOptimizer.kt
│   ├── NetworkOptimizer.kt
│   └── PerformanceConfig.kt
├── diagnostics/
│   ├── DiagnosticsManager.kt
│   ├── PerformanceSnapshot.kt
│   └── CrashRecoveryManager.kt
└── chromium/performance/
    └── ChromiumPerformanceAdapter.kt
```

`PerformanceManager` coordinates monitoring, policy, and optimization through:

```text
Android signals
      ↓
ResourceManager (Phase 5: which profiles remain active?)
      ↓
PerformanceManager (Phase 8: how do active profiles operate efficiently?)
      ↓
ProfileLifecycleManager
      ↓
Chromium
```

Do not create a second competing resource-management system.

## Performance modes

Support Performance, Balanced, and Battery Saver modes, with **Balanced** as the default. Performance prioritizes responsiveness; Balanced trades responsiveness, battery, and thermal behavior; Battery Saver reduces unnecessary background activity without overriding Android restrictions.

## RAM and CPU efficiency

Create `MemoryOptimizer` to reduce unnecessary allocations, release unused runtime objects, reduce inactive-tab resources, coordinate with profile suspension, avoid duplicate caches, and monitor pressure. Do not clear useful browser cache simply because RAM usage is high; storage cache and RAM are different resources.

Use pressure levels `NORMAL`, `LOW`, `MODERATE`, `HIGH`, and `CRITICAL`. Reduce background resources progressively, freeze inactive tabs at moderate pressure, suspend low-priority profiles at high pressure, and retain only essential active resources at critical pressure. Never delete profile data automatically.

Create `CpuOptimizer` to reduce unnecessary polling, background jobs, database queries, UI updates, resource scans, and network retries. Avoid continuous high-frequency monitoring. Audit every background task for visibility, active-state, event-driven, delayable, and cancellable behavior. Avoid uncontrolled loops and 50/100 ms polling unless measured evidence proves they are necessary.

## Tab and profile resource management

Inactive tabs should move through resource reduction while preserving URL, title, history position, profile ID, tab order, and session metadata:

```text
Active → Idle → Frozen → Suspended
                         ↓
                    Restore on use
```

The ResourceManager remains responsible for deciding which profiles stay active. Phase 8 optimizes active profiles and cooperates with Phase 5 lifecycle transitions. Saved profiles must not be treated as fully active Chromium environments.

## UI, threading, and database performance

Maintain smooth interaction across the address bar, tab switcher, profile switcher, menus, settings, history, bookmarks, and downloads. Never perform expensive operations on the Android main thread. Move database work, profile loading/saving, session serialization, large queries, download processing, resource analysis, suspension, and restoration to appropriate background execution.

Create `DatabaseOptimizer`. Audit Room/SQLite queries and add only useful indexes, including profile ID, creation/active timestamps, URL, and origin where justified. Batch or debounce writes rather than writing on every scroll, keystroke, or minor UI update, while still saving critical profile/session data safely.

History requires pagination, indexed searches, batched deletion, and efficient date grouping. Bookmarks use folder- and profile-based queries. Downloads use Android-supported background mechanisms and throttle progress updates to a reasonable frequency so UI updates do not occur hundreds of times per second.

## Network, battery, GPU, and thermal behavior

Create `NetworkOptimizer` with reasonable timeouts, bounded retries, connection-state handling, and reduced background activity. Suspended profiles must not repeatedly reconnect; resumption restores configured Phase 3 network behavior.

Create `BatteryOptimizer` to reduce background profile activity, unnecessary polling, and non-critical tasks at low battery, without interfering unnecessarily with active browsing. Create `ThermalOptimizer` using Android thermal status. Reduce background activity at light/moderate state, suspend more inactive profiles at severe state, and minimize background work at critical state. Do not bypass Android thermal protection.

Use Chromium-supported hardware acceleration. Do not disable GPU acceleration globally without measured evidence of a real improvement that does not break websites.

Network retries must be bounded:

```text
Attempt → failure → backoff → retry → maximum attempts → stop
```

## Startup and lazy loading

Create `StartupProfiler` to measure application startup, MainActivity startup, profile loading, database initialization, Chromium initialization, cold/warm startup, tab restoration, and first usable browser screen. Establish realistic internal targets from actual test devices instead of guaranteeing fixed numbers.

Load profile metadata first, then only the active profile, then display the browser. Load other profile runtime only when needed. Lazy-load feature components such as downloads, history, bookmarks, and settings instead of initializing every feature at startup. Do not preload many profiles; any preloading must be controlled by ResourceManager.

Optimize tab and profile switching without unnecessary recreation or full application restart. Profile switching saves required state, transitions lifecycle, loads the target profile, applies network and identity configuration, and restores browser state.

## Cache and cleanup strategy

Use Chromium's normal cache mechanisms and avoid duplicate page, image, or HTTP caches unless there is a measurable architectural reason. Audit for Activity/context leaks, WebContents references, tab/profile references, listeners, observers, coroutines, threads, and timers.

When a tab closes, save required state, detach UI, release Chromium resources, remove listeners, cancel tasks, release references, and delete the runtime object. When a profile closes, save the session, stop runtime, release resources, and retain persistent profile data.

## Crash recovery

Create `CrashRecoveryManager` to handle application, Chromium, tab, profile-runtime, and database failures while prioritizing user data. Database operations validate, execute in Room transactions, and verify success so profile metadata is not partially updated.

If a profile repeatedly crashes, mark it recovery-required, prevent automatic infinite restarts, and offer Retry, Start without problematic tabs, or Repair profile. Never delete the profile automatically.

## Diagnostics and configuration

Expose diagnostics under:

```text
Settings → Advanced → Diagnostics
```

Show app and Chromium versions, RAM pressure, CPU state, thermal state, battery state, active profiles/tabs, frozen profiles, and suspended profiles. Keep diagnostics separate from the normal browser menu.

Create `PerformanceSnapshot` containing timestamp, memory pressure, CPU state, thermal state, battery level, active profile count, active tab count, frozen profile count, and suspended profile count. It must not contain passwords, cookies, authentication tokens, private browsing content, or proxy credentials.

Create `PerformanceConfig` for centralized flags such as monitoring, diagnostics, aggressive background suspension, battery optimization, and thermal optimization. Release builds must not expose verbose internal logs by default.

Classify devices as `LOW_RESOURCE`, `STANDARD`, or `HIGH_RESOURCE` only to tune policy. Do not assume devices with the same model name have identical resources.

## Testing strategy

| Test area | Coverage |
|---|---|
| UI smoothness | Tab/profile switching, menus, address bar, scrolling, settings, history, bookmarks, downloads |
| Startup | Cold/warm startup, profile startup, database/Chromium initialization, first usable page |
| Stress | Many profiles, many tabs, repeated switching, suspend/resume, downloads, background/foreground transitions |
| Long-run | Repeated tabs, profile switching, network changes, downloads, history/bookmark growth |
| Battery | Normal browsing, multiple/background profiles, downloads, long idle |
| Thermal | Heavy browsing, multiple profiles, long downloads, video, repeated switching |
| Regression | Phase 1 browser shell, Phase 2 profiles, Phase 3 network, Phase 4 identity, Phase 5 lifecycle, Phase 6 privacy/security, Phase 7 features |

Investigate memory, CPU, thread, database-growth, and resource-accumulation leaks. Test low-RAM, mid-range, and high-end devices across Wi-Fi, mobile, poor networks, high thermal state, and low battery.

## Performance acceptance checklist

- [ ] Startup performance is measured and improved.
- [ ] UI remains responsive and expensive work stays off the main thread.
- [ ] RAM, CPU, battery, and thermal behavior are profiled.
- [ ] Background work is reduced appropriately.
- [ ] Inactive tabs/profiles release resources while active tabs remain responsive.
- [ ] Tab and profile switching are optimized.
- [ ] Database, history, bookmarks, downloads, and network operations are efficient.
- [ ] Network retries are bounded.
- [ ] Chromium resources are released correctly.
- [ ] Crash recovery and crash-loop protection work.
- [ ] Low-resource devices receive conservative policies.
- [ ] High-resource devices use more runtime resources safely.
- [ ] Diagnostics exclude sensitive information.
- [ ] Stress, long-run, battery, thermal, and regression tests pass.
- [ ] Phase 1–7 functionality remains intact.

## Explicit exclusions

Do not implement Play Store publishing, production release signing, final CI/CD release pipeline, public release artifacts, store listing, production legal package, marketing assets, telemetry rollout, anti-bot or anti-fraud bypass, CAPTCHA bypass, guaranteed anonymity, or guaranteed undetectability in Phase 8.

## End state

PHNX Browser is fast, stable, RAM-efficient, CPU-efficient, battery-aware, thermally aware, and profile-aware. Performance optimization works alongside browser features, profile isolation, network, device configuration, privacy/security, and the Phase 5 ResourceManager without silently removing functionality.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Performance architecture](./diagrams/rendered/performance-architecture.png)
- [Pressure-response policy](./diagrams/rendered/pressure-policy.png)
- [Startup and lazy loading](./diagrams/rendered/startup-lazy-loading.png)
- [Profile switching optimization](./diagrams/rendered/profile-switch-optimization.png)
- [Diagnostics and testing](./diagrams/rendered/diagnostics-testing.png)
