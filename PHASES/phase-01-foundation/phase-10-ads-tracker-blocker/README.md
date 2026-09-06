# PHNX Browser — Phase 10 Blueprint

## Built-in Ads and Tracker Blocker

> **Scope:** Add a built-in, profile-aware content blocker that users can control from **Settings → Privacy & Security → Ads & Tracker Blocking**, while preserving the profile, network, privacy, lifecycle, performance, and release architecture established in Phases 1–9.

> **Accuracy rule:** Report actual blocked network requests, not visual ads removed, unless the implementation truly removes page elements. Never show a fake blocking status.

## User experience

The primary control is a clear `Block ads and trackers` switch. When disabled, Chromium page behavior proceeds normally and PHNX applies no blocking rules. When enabled, web requests pass through the supported Chromium interception layer and are evaluated by the local blocker.

```text
Web request
    ↓
ChromiumAdBlockAdapter
    ↓
AdBlockManager
    ↓
Rule engine
    ↓
BLOCK or ALLOW
```

Settings should include advertising, tracker, and known malicious-ad controls, lightweight request statistics, and profile-specific site exceptions. The main switch must remain sufficient for normal users, with advanced controls below it.

## Architecture

Suggested package structure:

```text
app/src/main/java/com/phoenix/phnx/
├── adblock/
│   ├── AdBlockManager.kt
│   ├── AdBlockSettings.kt
│   ├── AdBlockRuleEngine.kt
│   ├── AdBlockRuleRepository.kt
│   ├── AdBlockFilter.kt
│   ├── TrackerBlocker.kt
│   ├── BlockedRequest.kt
│   ├── BlockStatistics.kt
│   ├── SiteExceptionManager.kt
│   └── AdBlockRepository.kt
└── chromium/adblock/
    └── ChromiumAdBlockAdapter.kt
```

Use real Chromium integration. Do not report that a request was blocked unless the underlying interception mechanism actually prevented it.

## Profile-scoped settings

Create `AdBlockSettings` with:

```text
enabled
blockAds
blockTrackers
blockMaliciousAds
siteExceptions
```

Defaults are enabled, with advertising, tracker, and malicious-ad blocking enabled. Store the configuration with the profile. Profile A may have blocking enabled while Profile B has it disabled; one profile's settings must never affect another.

## Rule engine

Use a maintained filter-list format suitable for the actual Chromium integration used by PHNX. The rule engine may evaluate URL, domain, resource type, request context, first-party domain, and third-party domain. Candidate resource types include scripts, images, stylesheets, fonts, media, XHR, fetch, subframes, and other supported categories, but PHNX must implement only types exposed by the underlying interception API.

Keep rules as validated data, never executable code. Treat downloaded lists as untrusted input and validate format, size, integrity, parser safety, malformed-rule handling, resource-exhaustion resistance, and the absence of unexpected code execution.

## Filter lists and updates

Use reputable, legally usable advertising, tracking, malicious-ad, and privacy lists. Check licensing and redistribution requirements before bundling or distributing lists. Do not blindly include arbitrary third-party lists.

Update rules safely in the background:

```text
Download → validate → store → activate
```

Keep the old working ruleset until the new ruleset passes validation. If download or validation fails, retain the old ruleset. Existing rules continue operating offline; the blocker must not require an online request for every ad check.

## Site exceptions and quick controls

Provide profile-specific exceptions under:

```text
Settings → Privacy & Security → Ads & Tracker Blocking → Site Exceptions
```

Users can allow ads and trackers on the current site, remove an exception, and reload the page after changing the policy. Optionally expose a shield indicator beside the address bar that opens a site-specific Block/Allow control. Allowing a site adds it to the current profile's exception list.

When a site breaks, the recovery flow is:

```text
Site breaks → disable blocking for this site → reload page
```

Minimize breakage with allow rules, exceptions, first-party exceptions, and resource-type matching. Do not disable JavaScript globally; JavaScript remains ON by default and blocking operates independently.

## Statistics and diagnostics

Track lightweight, accurate metrics such as requests blocked today, this week, and this month, with separate tracker and ad counts where supported. Label these metrics as **Requests blocked** rather than **Ads removed** unless visual element removal is actually implemented.

Do not store browsing history, full URLs, or unnecessary page data in statistics. Under `Settings → Advanced → Diagnostics → Ads & Tracker Blocking`, show blocker status, ruleset version, rule count, evaluated requests, blocked requests, and last update without exposing unnecessary browsing information.

## Integration with previous phases

Integrate with Phase 6 without duplicating responsibilities:

```text
PrivacyManager
├── TrackingProtectionManager
└── AdBlockManager
```

Tracking protection and ad blocking should have clear boundaries and avoid duplicate processing where possible. The blocker works with both DIRECT and PROXY profile network configurations without modifying Phase 3 proxy behavior.

Integrate with Phase 5 and Phase 8 resource management. Use compiled rules, memory-efficient lookup, caching, background updates, and bounded processing. Under memory pressure, release optional statistics and decision caches before changing the user's blocker preference. Do not automatically disable blocking merely because RAM is low unless the user explicitly selects a performance mode.

Avoid huge per-request database queries, UI-thread work, and recompiling filter rules for every request. Use efficient data structures and cached decisions to avoid excessive CPU and thermal load.

## Security and privacy requirements

Filter-list downloads are untrusted input. Validate them before activation and protect against malformed rules, oversized inputs, parser vulnerabilities, resource exhaustion, and executable content. Secrets, credentials, cookies, full page contents, and unnecessary browsing details must not appear in statistics, diagnostics, logs, or exported data.

The safe default is blocking ON, but users must have a clear way to turn it OFF. Disabling blocking for a specific site should be easier than navigating through advanced settings.

## Testing and acceptance

| Test area | Expected result |
|---|---|
| Main toggle | ON/OFF changes actual blocker behavior and persists. |
| Profile isolation | Profile A's blocker settings and exceptions do not affect Profile B. |
| Blocking | Supported ads and trackers are actually blocked by Chromium integration. |
| JavaScript | JavaScript remains ON by default and independent from blocking. |
| Exceptions | Site-specific allow/block controls work and trigger reload where appropriate. |
| Rule updates | New rules validate before activation; invalid downloads preserve the old ruleset. |
| Offline | Installed rules continue to work without network access. |
| Statistics | Counts represent blocked requests and do not expose unnecessary browsing data. |
| Performance | Rule matching, caching, RAM, CPU, and thermal behavior are acceptable on mobile devices. |
| Network | DIRECT, PROXY, network loss/recovery, and profile switching remain functional. |
| Release | CI tests pass and release builds integrate the real blocker. |

Acceptance requires the settings UI, working main switch, ON default, persistence, profile scope, real ad/tracker blocking, preserved JavaScript behavior, site exceptions, safe updates, offline operation, accurate statistics, controlled resource use, real Chromium integration, no fake status, passing CI, and a working release build.

## Settings structure

```text
Settings
├── Profiles
├── Privacy & Security
│   ├── Cookies
│   ├── Tracking Protection
│   ├── Ads & Tracker Blocking
│   ├── Site Permissions
│   ├── Clear Browsing Data
│   └── Private Browsing
├── Network
├── Performance
├── Search
├── Tabs
├── Downloads
├── Appearance
├── Accessibility
├── Site Settings
├── Advanced
└── About PHNX
```

## Explicit exclusions

Do not implement anti-bot, anti-fraud, CAPTCHA, access-control, or detection-evasion bypasses. Do not claim that blocking provides guaranteed anonymity, guaranteed undetectability, or complete privacy. Do not silently disable JavaScript, alter proxy behavior, bypass Chromium validation, or report unsupported blocking capabilities.

## End state

PHNX Browser provides a user-controllable, profile-aware ad and tracker blocker that operates locally with validated rules, safe background updates, site exceptions, accurate statistics, offline behavior, Chromium integration, and measured mobile performance. It remains compatible with the existing profile isolation, network, privacy, lifecycle, performance, and release systems.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Blocker architecture](./diagrams/rendered/blocker-architecture.png)
- [Request decision flow](./diagrams/rendered/request-decision-flow.png)
- [Profile configuration isolation](./diagrams/rendered/profile-blocker-isolation.png)
- [Safe ruleset update](./diagrams/rendered/ruleset-update.png)
- [Privacy and performance integration](./diagrams/rendered/privacy-performance-integration.png)
