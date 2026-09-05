# PHNX Browser — Phase 7 Blueprint

## Complete Browser Features

> **Scope:** Complete PHNX Browser's everyday Android browser functionality so it provides a full, profile-aware experience across tabs, history, bookmarks, downloads, search, page controls, sharing, shortcuts, and session restoration.

> **Integration rule:** Every feature must work with the profile, network, identity, privacy, security, and lifecycle systems established in Phases 2–6.

> **Safety boundary:** Do not implement anti-bot, anti-fraud, CAPTCHA bypass, guaranteed anonymity, guaranteed undetectability, or other evasion mechanisms.

## Objective

Phase 7 completes the everyday browser surface: profile-aware tabs, private tabs, tab groups, history, bookmarks, downloads, recent tabs, search, a start page, find-in-page, desktop-site mode, zoom, text scaling, full-screen content, sharing, home-screen shortcuts, organized settings, and robust browser error handling.

## Architecture

Suggested package structure:

```text
app/src/main/java/com/phoenix/phnx/
├── tabs/
│   ├── Tab.kt
│   ├── TabManager.kt
│   ├── TabRepository.kt
│   ├── TabGroup.kt
│   ├── TabSwitcher.kt
│   └── TabRestorationManager.kt
├── history/
│   ├── HistoryManager.kt
│   ├── HistoryRepository.kt
│   └── HistoryEntry.kt
├── bookmarks/
│   ├── BookmarkManager.kt
│   ├── BookmarkRepository.kt
│   ├── BookmarkFolder.kt
│   └── BookmarkItem.kt
├── downloads/
│   ├── DownloadManager.kt
│   ├── DownloadRepository.kt
│   └── DownloadItem.kt
├── search/
│   ├── SearchEngineManager.kt
│   └── SearchEngine.kt
├── pages/
│   ├── FindInPageManager.kt
│   ├── PageZoomManager.kt
│   ├── TextScaleManager.kt
│   └── DesktopSiteManager.kt
├── sharing/
│   └── ShareManager.kt
├── shortcuts/
│   └── HomeScreenShortcutManager.kt
└── ui/
    ├── StartPage.kt
    ├── TabSwitcherActivity.kt
    ├── HistoryActivity.kt
    ├── BookmarksActivity.kt
    └── DownloadsActivity.kt
```

Business logic belongs in dedicated managers and controllers, not directly in menu or presentation classes.

## Tabs and sessions

Every `Tab` belongs to exactly one profile. Its model should include:

```text
Tab
├── id
├── profileId
├── url
├── title
├── favicon
├── isPrivate
├── isActive
├── isLoading
├── canGoBack
├── canGoForward
├── createdAt
├── lastActiveAt
└── state
```

Expand `TabManager` with profile-aware operations:

```text
createTab(profileId)
createPrivateTab(profileId)
closeTab(tabId)
closeAllTabs(profileId)
switchTab(tabId)
getActiveTab(profileId)
getTabs(profileId)
moveTab(tabId, position)
restoreTab(tabId)
```

Provide a simple Chrome-like tab switcher supporting open, switch, close, new tab, private tab, and basic tab groups. A `TabGroup` contains an ID, profile ID, title, tabs, and creation time. Tabs from different profiles must never share a group.

`TabRestorationManager` saves URL, title, profile ID, private state, tab order, and active tab. On restart, load profiles and restore only the sessions required by Phase 5 resource management; do not activate every saved profile automatically. Recently closed tabs store URL, title, profile ID, and closed time, while private tabs follow private-session rules and must not become normal persistent tabs after the session ends.

## History

Create `HistoryManager` with:

```text
addEntry()
getHistory()
searchHistory()
deleteEntry()
deleteRange()
clearHistory()
```

Each `HistoryEntry` includes ID, profile ID, URL, title, visit time, and visit count. History is profile-specific by default. History UI should group entries into Today, Yesterday, and Earlier, show favicon/title/domain/time, and support open, delete, search, and clear operations. Search covers title, URL, and domain for the selected profile unless the user explicitly requests a cross-profile view.

## Bookmarks

Create `BookmarkManager` with add, remove, update, list, move, and folder operations. `BookmarkItem` includes ID, profile ID, folder ID, title, URL, favicon, and creation time. Support folders such as Mobile, Work, Entertainment, and Other, with create, rename, move, and delete actions. Clearing cache must never delete bookmarks. Provide Add to bookmarks and Edit bookmark actions from the browser menu.

## Downloads

Create `DownloadManager` and `DownloadItem` with profile ID, URL, filename, MIME type, size, downloaded bytes, status, creation time, and local path. Use Android-supported download and storage mechanisms.

Statuses are `QUEUED`, `DOWNLOADING`, `PAUSED`, `COMPLETED`, `FAILED`, and `CANCELLED`. The Downloads screen supports open, pause, resume, cancel, delete, and share. Show progress and Android notifications where appropriate, respect notification permissions, and never execute downloaded files automatically.

## Search and start page

Create `SearchEngineManager` and a configurable `SearchEngine` model with ID, name, search URL, and suggestion URL. Provide a default search engine without hardcoding the architecture around one provider.

The address bar must distinguish direct URLs from search queries. URLs navigate directly; other text is sent to the selected search engine. Suggestions may be provided where supported, but private typed information must not be sent to a suggestion provider unless the configured behavior allows it. The start page includes PHNX Browser branding, a search/address field, bookmarks, recently visited items, and quick shortcuts without creating unnecessary news or tracking feeds by default.

New Tab opens the configured start page; New Private Tab opens a private start page.

## Page controls

| Feature | Requirement |
|---|---|
| Find in page | `find()`, `next()`, `previous()`, and `clear()` with match count where supported. |
| Desktop site | Profile/site-aware preference where Chromium supports it. |
| Page zoom | Chromium-supported zoom levels from 50% through 200%, with profile/site persistence where appropriate. |
| Text scaling | Small, Default, Large, and Very large using supported Android/Chromium mechanisms. |
| Full-screen | Support website full-screen requests where permitted and provide a reliable exit. |
| Address bar | Display active-tab URL and support select-all, paste, copy, cut, clear, type, and submit. |
| Reload | Operate on the currently active tab only. |

## Browser menu and sharing

The three-dot button must remain immediately after the address/domain area. Menu items are:

```text
New tab
New private tab
Bookmarks
History
Downloads
Recent tabs
Share
Find in page
Desktop site
Add to Home screen
Settings
About PHNX
```

Each menu item calls a dedicated controller:

```text
NewTab → TabManager
PrivateTab → PrivateBrowsingManager
Bookmarks → BookmarkManager/UI
History → HistoryManager/UI
Downloads → DownloadManager/UI
RecentTabs → TabRestorationManager
Share → ShareManager
Find → FindInPageManager
DesktopSite → DesktopSiteManager
HomeShortcut → HomeScreenShortcutManager
Settings → SettingsActivity
About → AboutActivity
```

Do not place RAM, CPU, thermal, developer, database, or network-internal controls in the normal menu; those remain in Settings. `ShareManager` should use Android's standard share mechanism and share page title and URL. `HomeScreenShortcutManager` creates supported launcher shortcuts containing title, URL, icon, and profile ID, opening in the intended profile where Android permits.

## Profile-aware integration

The following remain tied to the current profile: tabs, history, bookmarks, cookies, site permissions, downloads where applicable, privacy settings, network settings, identity configuration, and session state. Do not accidentally use another profile's database.

Profile switching invokes the existing systems in sequence:

```text
ProfileManager
      ↓
NetworkManager
      ↓
DeviceProfileManager
      ↓
Privacy and permissions
      ↓
LifecycleManager
      ↓
Active profile
```

Use the existing Phase 6 `ClearDataManager` for history, cookies, cache, site storage, permissions, and downloads where applicable. Do not create a second data-clearing engine. Features must cooperate with Phase 5 so inactive tabs and profiles can be frozen or suspended rather than forced to remain fully active.

## Settings, appearance, accessibility, and errors

Organize Settings into Profiles, Privacy & Security, Network, Performance, Search, Tabs, Downloads, Appearance, Accessibility, Site Settings, Advanced, and About PHNX. Provide Light, Dark, and System default themes, plus modest toolbar options such as showing the home button or bookmarks where supported.

Follow Android accessibility standards: label buttons, support keyboard and accessibility navigation, maintain readable contrast, provide usable touch targets, and ensure screen readers such as TalkBack can identify controls.

Provide PHNX error handling for no internet, DNS failure, connection failure, timeout, certificate error, unavailable pages, and download failure. Explain what happened and provide Retry without exposing internal stack traces. When offline, show a connection error and avoid aggressive repeated retries. If an individual tab crashes, preserve its metadata and offer Reload without crashing the full application where the underlying Chromium architecture allows it.

## Testing

| Test area | Scenarios |
|---|---|
| Tabs | Create, switch, close, restore, private tabs, and tab groups. |
| History | Add, search, delete, clear, and profile isolation. |
| Bookmarks | Add, edit, delete, folders, move, and profile isolation. |
| Downloads | Start, pause, resume, cancel, complete, and failure. |
| Search | URL detection, search query routing, and search-engine changes. |
| Page features | Find in page, zoom, desktop site, text scaling, and full-screen. |
| Profile switching | Verify every browser feature uses the correct profile. |
| Error recovery | Offline state, failed navigation, and tab crash recovery. |

### End-to-end test

Create Profile A, open five tabs, visit sites, bookmark pages, search, download a file, inspect history, switch to Profile B, open different tabs, return to Profile A, and verify all state is restored. Restart PHNX and verify both profiles and their sessions remain correctly separated.

## Acceptance checklist

- [ ] Full profile-aware tab system works.
- [ ] Private tabs, restoration, recently closed tabs, and tab groups work.
- [ ] History, search, clearing, and profile isolation work.
- [ ] Bookmarks, folders, moving, and profile isolation work.
- [ ] Downloads, progress, notifications, and safe actions work.
- [ ] Search engine selection and URL/search detection work.
- [ ] Start page works.
- [ ] Find in page, desktop site, zoom, text scaling, and full-screen work where supported.
- [ ] Android sharing and home-screen shortcuts work where supported.
- [ ] Profile switching works across all browser features.
- [ ] Browser menu remains organized with `⋮` immediately after the address/domain area.
- [ ] Settings, accessibility, error pages, offline behavior, and tab crash recovery work.
- [ ] Private browsing data remains separate from normal browsing.
- [ ] Phase 1–6 functionality remains intact.
- [ ] Tests pass.

## Explicit exclusions

Do not implement final production optimization, advanced thermal tuning, release signing, CI/CD release pipeline, Play Store deployment, production crash analytics, final legal release package, advanced telemetry, anti-bot or anti-fraud bypass, CAPTCHA bypass, guaranteed anonymity, or guaranteed undetectability in Phase 7.

## End state

PHNX Browser now functions as a complete everyday Android browser with address and navigation controls, tabs, private tabs, tab groups, history, bookmarks, downloads, recent tabs, search, find-in-page, desktop site, zoom, text scaling, sharing, home-screen shortcuts, profiles, privacy/security, network, performance, and organized settings.

```text
UI
 ↓
Feature managers
 ↓
ProfileManager
 ↓
Privacy / Network / Identity / Lifecycle
 ↓
Chromium
```

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Feature architecture](./diagrams/rendered/feature-architecture.png)
- [Profile-aware browser data](./diagrams/rendered/profile-aware-data.png)
- [Tab and session restoration](./diagrams/rendered/tab-restoration.png)
- [Browser menu routing](./diagrams/rendered/menu-routing.png)
- [End-to-end browser flow](./diagrams/rendered/end-to-end-flow.png)
