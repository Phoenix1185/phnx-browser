# PHNX Browser — Phase 6 Blueprint

## Privacy, Security, and Permissions

> **Scope:** Build PHNX Browser's privacy, security, permission, and data-protection layer using supported Android and Chromium APIs. Users must have clear control over permissions, cookies, storage, tracking protection, HTTPS warnings, private browsing, downloads, JavaScript, pop-ups, third-party content, and profile data.

> **Accuracy rule:** Do not claim 100% privacy, complete anonymity, or undetectability. Private browsing primarily controls local browser persistence and does not hide activity from internet providers, network administrators, websites, proxy operators, device administrators, or other network observers.

## Architecture

Suggested package structure:

```text
app/src/main/java/com/phoenix/phnx/
├── privacy/
│   ├── PrivacyManager.kt
│   ├── PrivacySettings.kt
│   ├── TrackingProtectionManager.kt
│   ├── CookieManager.kt
│   ├── StorageManager.kt
│   ├── PrivateBrowsingManager.kt
│   └── PrivacyRepository.kt
├── security/
│   ├── SecurityManager.kt
│   ├── CertificateManager.kt
│   ├── SafeBrowsingManager.kt
│   ├── SecurityState.kt
│   └── SecurityEvent.kt
├── permissions/
│   ├── PermissionManager.kt
│   ├── PermissionRepository.kt
│   ├── PermissionRequest.kt
│   ├── PermissionState.kt
│   └── PermissionType.kt
├── downloads/
│   └── DownloadSecurityManager.kt
└── chromium/
    ├── privacy/ChromiumPrivacyAdapter.kt
    ├── security/ChromiumSecurityAdapter.kt
    └── permissions/ChromiumPermissionAdapter.kt
```

Only expose settings that the integrated Chromium build actually supports.

## Privacy settings and profile scope

Create `PrivacySettings` with supported values such as third-party-cookie policy, tracking protection, Do Not Track preference, JavaScript, pop-ups, notifications, location, camera, microphone, clipboard policy, safe browsing, clear-data-on-exit, and private-browsing behavior.

Profile-specific data must remain inside the profile boundary:

```text
Profile A                         Profile B
├── Privacy settings A             ├── Privacy settings B
├── Permissions A                 ├── Permissions B
└── Storage A                     └── Storage B
```

Changing Profile A's site permissions, cookies, storage, privacy settings, network settings, or identity settings must not change Profile B. Keep global settings such as theme, download location, general UI, and default search engine separate from profile settings.

## Permission system

Create `PermissionManager` with:

```text
requestPermission()
grantPermission()
denyPermission()
revokePermission()
getPermissionState()
getPermissionsForSite()
clearPermissionsForSite()
clearAllPermissions()
```

Support only permissions exposed by Android/Chromium, including camera, microphone, location, notifications, MIDI, clipboard, and other supported types. Sensitive permissions must never be granted silently.

Permission decisions are keyed by:

```text
profileId + origin + permissionType
```

For example, `example.com` camera access may be blocked in Profile A and allowed in Profile B without leakage.

When a website requests permission, Chromium passes the request to PHNX, which checks stored site permission and then shows a clear prompt if no decision exists. Where supported, provide Allow, Block, Allow this time, and Allow while visiting. Camera and microphone require both Chromium and Android permission approval; a website permission cannot override an Android denial. Location follows the same two-layer rule. Website notifications must also respect Android notification permission requirements.

## Permission settings UI

Expose site permissions through:

```text
Settings → Privacy & Security → Site Permissions
```

Show Camera, Microphone, Location, Notifications, Pop-ups, JavaScript, Clipboard, and Downloads categories. Users must be able to view sites, allow, block, and reset decisions.

## Cookies, storage, and clear-data operations

Use Chromium's supported cookie and storage APIs rather than creating a custom cookie engine. `CookieManager` should support viewing policy, clearing site cookies, clearing all cookies, and third-party-cookie control where supported.

`StorageManager` and `ClearDataManager` should account for cookies, LocalStorage, IndexedDB, cache, service workers, WebSQL where applicable, browsing history, downloads, permissions, and other supported site data. Clearing data must be profile-specific. Support time ranges where the underlying API permits them, and document the closest supported behavior otherwise.

Before destructive clearing, show exactly what will be removed and what will remain. Clearing browsing data is separate from deleting a profile and must not silently remove bookmarks or the saved profile.

## Private browsing

Create `PrivateBrowsingManager`. Private tabs should avoid persistent history, avoid persistent cookies and website storage where supported, isolate private state from normal profiles, and close private session data when the session ends.

Add **New private tab** to the main menu and clearly identify private mode. The explanation must remain accurate and must not imply network anonymity or protection from external observers.

## Tracking protection and third-party content

Create `TrackingProtectionManager` with supported levels such as `OFF`, `STANDARD`, and `STRICT`; use `STANDARD` as the default where available. Control third-party cookies, storage, tracking resources, and cross-site requests only through supported Chromium mechanisms. Do not break normal websites unnecessarily; if Strict mode causes compatibility issues, provide a clear troubleshooting path.

If supported, expose Do Not Track as a browser preference and explain that websites are not required to obey it. It must not be presented as guaranteed tracker blocking.

## HTTPS and security state

Create `CertificateManager` and rely on standard Android/Chromium certificate validation. Invalid certificates must not be silently bypassed or presented as safe. Show a clear warning instead.

Create `SecurityState` with values such as `SECURE`, `NOT_SECURE`, `CERTIFICATE_ERROR`, `MIXED_CONTENT`, and `UNKNOWN`. The address bar must reflect actual Chromium security state, not merely the presence of `https` in a URL. Do not show a secure indicator when the connection is not actually secure.

Create `SafeBrowsingManager` using supported Chromium/Android safe-browsing functionality. If a site is identified as dangerous, show a warning and do not silently continue. Do not claim to detect every malicious site.

## Download security, pop-ups, and JavaScript

Create `DownloadSecurityManager` to check URL/security state where supported, file metadata, and destination before downloading. Do not automatically execute downloaded files. For potentially dangerous files, rely on Android security mechanisms and provide appropriate warnings.

Pop-ups should default to Blocked where supported, with site-specific exceptions. JavaScript remains enabled by default and can be controlled through Site Settings with Allowed/Blocked and per-site exceptions where supported. Do not disable JavaScript globally as a hidden privacy mechanism.

Clipboard access must follow Android/Chromium rules and must not receive unrestricted silent grants.

## Password and profile data protection

Do not log passwords or store them in plain SQLite. Do not expose credentials through diagnostic screens. Use Android Keystore-backed storage for secrets where required.

All profile-sensitive data remains within its profile boundary:

```text
Profile A                         Profile B
├── Cookies A                     ├── Cookies B
├── History A                     ├── History B
├── Storage A                     ├── Storage B
├── Permissions A                 ├── Permissions B
└── Session A                     └── Session B
```

## Security events and dashboards

Create `SecurityEvent` values such as `CERTIFICATE_ERROR`, `PERMISSION_GRANTED`, `PERMISSION_DENIED`, `SAFE_BROWSING_WARNING`, `DOWNLOAD_BLOCKED`, `SECURITY_POLICY_CHANGED`, and `PROFILE_SECURITY_RESET`. Security logs must not store sensitive page contents, cookies, passwords, proxy credentials, authentication tokens, or private browsing data.

Create a clear dashboard under:

```text
Settings → Privacy & Security
```

Include Privacy, Security, Permissions, Cookies, Tracking Protection, Site Settings, Clear Browsing Data, Private Browsing, and Safe Browsing. Keep advanced diagnostics separate and the normal user interface understandable.

Provide Profile → Privacy & Security → Reset Site Permissions with confirmation for resetting permissions, cookies, site storage, browsing data, or all privacy settings. Profile deletion remains distinct from data clearing and requires explicit confirmation.

## Integration with Phases 3–5

Keep `NetworkManager`, `PrivacyManager`, `PermissionManager`, `SecurityManager`, and `DeviceProfileManager` as separate modular services. Privacy changes must not overwrite network configuration, and network changes must not reset permissions.

Lifecycle transitions must preserve privacy and security state. After a profile resumes, its cookies, permissions, privacy settings, network configuration, and identity configuration must remain attached to the same profile without silent resets.

## Automated testing

| Test area | Expected result |
|---|---|
| Permissions | Camera, microphone, and location grant/deny/revoke flows respect Android and Chromium decisions. |
| Permission isolation | Profile B does not inherit Profile A's site permissions. |
| Cookies | A cookie in Profile A is unavailable in Profile B. |
| Storage | LocalStorage, IndexedDB, cache, service workers, and supported storage remain isolated. |
| Private browsing | Private sessions do not populate normal history where supported. |
| Security | HTTPS, HTTP, invalid certificates, mixed content, and safe-browsing warnings reflect actual state. |
| Clear data | Clearing Profile A does not remove Profile B data. |
| Lifecycle | Suspend/resume preserves privacy configuration. |
| Logging | Sensitive information is absent from logs. |

## Acceptance checklist

- [ ] Privacy & Security settings exist.
- [ ] Site permissions are implemented and profile-specific.
- [ ] Camera, microphone, location, and notification permissions are protected.
- [ ] Cookies and website storage can be controlled and cleared.
- [ ] Third-party-cookie controls work where supported.
- [ ] Tracking protection works where supported.
- [ ] JavaScript remains enabled by default and can be controlled.
- [ ] Pop-up controls work.
- [ ] Private browsing works with accurate disclosure.
- [ ] HTTPS indicators reflect actual Chromium security state.
- [ ] Certificate errors are not silently bypassed.
- [ ] Safe browsing works where supported.
- [ ] Downloads receive appropriate security handling.
- [ ] Clear browsing data works without deleting unrelated profile data.
- [ ] Profile data remains isolated.
- [ ] Privacy settings survive lifecycle transitions.
- [ ] Sensitive information never appears in logs.
- [ ] Android and Chromium permissions are both respected.
- [ ] Tests pass.
- [ ] Existing Phase 1–5 functionality remains working.

## Explicit exclusions

Do not implement a complete download manager, advanced bookmark/history systems, recent-tab synchronization, share-system overhaul, find-in-page overhaul, home-screen shortcuts, advanced browser customization, release signing, Play Store publishing, anti-bot or anti-fraud bypass, CAPTCHA bypass, guaranteed anonymity, or guaranteed undetectability in Phase 6.

## End state

After Phase 6, PHNX Browser provides understandable privacy and security controls across permissions, cookies, storage, tracking protection, HTTPS security, safe browsing, private browsing, JavaScript, pop-ups, downloads, and profile data protection. The implementation remains honest about supported capabilities and preserves the isolation established in Phases 2–5.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Privacy and security architecture](./diagrams/rendered/privacy-security-architecture.png)
- [Permission request flow](./diagrams/rendered/permission-request-flow.png)
- [Profile privacy isolation](./diagrams/rendered/profile-privacy-isolation.png)
- [Security state flow](./diagrams/rendered/security-state-flow.png)
- [Clear-data safety flow](./diagrams/rendered/clear-data-safety.png)
