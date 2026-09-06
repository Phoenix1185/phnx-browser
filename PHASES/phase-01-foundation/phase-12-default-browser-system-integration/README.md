# PHNX Browser — Phase 12 Blueprint

## Default Browser and Android System Integration

> **Scope:** Make PHNX behave like a proper Android browser at the operating-system level. Users can optionally choose PHNX as their default browser, while Android permissions, browser roles, intents, links, and system interactions remain explicit, safe, and user-controlled.

> **Consent rule:** Never silently change the default browser, request unnecessary permissions, grant website permissions without appropriate authorization, or bypass Android's permission and role systems.

## Default-browser role

Expose default-browser controls under:

```text
Settings → Default Browser
```

Show the actual Android state:

```text
Default Browser
PHNX Browser
Status: ● Not the default browser
[Set PHNX as default]
```

When PHNX is already selected, show `PHNX is your default browser` and provide `Manage default browser`. The first-launch experience may offer the choice, but users can select **Not now** and continue normally.

Register the proper Android browser capabilities for `http://`, `https://`, and normal browser intents. When a user chooses Set PHNX as default, open the appropriate Android system role/settings interface rather than attempting to change the default silently.

## External links and intents

PHNX receives browser-compatible `ACTION_VIEW`, HTTP, and HTTPS intents from WhatsApp, Telegram, email, messages, social apps, and other applications. Parse URL, URI, extras, and referrer where available, but validate all incoming data and never blindly trust arbitrary extras.

When PHNX is not default, Android controls the chooser between PHNX, Chrome, and other browsers. PHNX must not override that choice.

External-link routing is profile-aware. The default behavior is to open links in the default profile. Optional settings may allow Always use default profile, Ask which profile, or Use last active profile. Do not expose private profile information unnecessarily in external intent handling.

## Profile selection for external links

```text
External link
      ↓
PHNX Browser
      ↓
Default profile or user-selected profile
      ↓
Validate URL
      ↓
Open in selected profile
```

The selected profile must retain its own cookies, permissions, network configuration, identity configuration, and session state. External intents must never expose credentials, cookies, or profile data.

## Android permissions center

Provide:

```text
Settings → Permissions
```

Show PHNX's actual Android permission state for camera, microphone, location, notifications, and photos/media where applicable. Each permission offers Allow, Deny, or a route to Android system settings where required. Add shortcuts for Open Android App Settings and Open Default Browser Settings.

Request permissions contextually and only when a feature needs them. Do not request camera, microphone, location, notifications, or storage access during first launch merely because they might be useful later.

The request flow is:

```text
Website requests access
      ↓
Chromium website-permission layer
      ↓
PHNX PermissionManager
      ↓
Android runtime permission
      ↓
User approval or denial
```

Denied permissions must be handled gracefully without crashing the tab or browser.

## Android and website permission separation

Keep Android app permissions separate from profile/site-scoped website permissions. Website permissions may include camera, microphone, location, notifications, clipboard, downloads, pop-ups, JavaScript, cookies, and sensors where supported.

Example site state:

```text
example.com
Camera        Blocked
Microphone    Allowed
Location      Ask
Notifications Blocked
```

A website notification permission must not be granted simply because Android notifications for PHNX are enabled. Location is OFF by default unless a user-approved feature requires it. Users can revoke permissions later.

## Central permission architecture

Create a single permission layer rather than duplicating logic across browser screens:

```text
app/src/main/java/com/phoenix/phnx/
├── system/
│   ├── DefaultBrowserManager.kt
│   ├── BrowserRoleManager.kt
│   ├── IntentHandler.kt
│   ├── ExternalLinkHandler.kt
│   └── SystemSettingsLauncher.kt
├── permissions/
│   ├── PermissionManager.kt
│   ├── AndroidPermissionManager.kt
│   ├── WebsitePermissionManager.kt
│   ├── PermissionPolicy.kt
│   ├── PermissionState.kt
│   └── PermissionSettings.kt
└── intents/
    ├── UrlIntentParser.kt
    ├── BrowserIntentHandler.kt
    └── ExternalNavigationHandler.kt
```

Responsibilities include `checkPermission()`, `requestPermission()`, `grantPermission()`, `denyPermission()`, `revokePermission()`, and `getPermissionState()`.

## Notifications, camera, microphone, files, and downloads

Distinguish PHNX notifications from website notifications and respect Android notification permission requirements. For WebRTC and camera/microphone sites, require both Chromium website authorization and Android runtime authorization.

Downloads and files follow Android storage rules, configured download locations, user-selected locations, the file picker, and applicable media permissions. Do not request broad storage access unnecessarily.

## URL handling and security

Handle `http://`, `https://`, and supported browser-compatible schemes. Validate URLs before navigation. Unsupported or dangerous schemes must not automatically execute.

The default-browser status is read dynamically from Android and represented as `DEFAULT`, `NOT_DEFAULT`, or `UNKNOWN/UNAVAILABLE`. Never hardcode the status.

Security requirements include:

- Never silently change the default browser.
- Never request unnecessary Android permissions.
- Never grant website permissions without appropriate user/system authorization.
- Keep website permissions profile-aware.
- Respect Android permission revocation.
- Handle denied permissions gracefully.
- Never expose credentials, cookies, or profile data through intents.
- Never bypass Android's permission system.

## Integration with previous phases

Phase 12 preserves the profile isolation from Phase 2, network behavior from Phase 3, device/browser configuration from Phase 4, lifecycle/resource management from Phase 5, privacy/security from Phase 6, complete browser features from Phase 7, performance/thermal management from Phase 8, production release hardening from Phase 9, built-in ads/tracker blocking from Phase 10, and self-update/patch support from Phase 11.

External links should use the selected profile's configured network, privacy, permissions, identity, lifecycle, and browser state without creating a second profile or permission system.

## Testing and acceptance

| Test area | Expected result |
|---|---|
| Default browser | PHNX can be selected, another browser can remain default, and status is dynamically detected. |
| Intents | HTTP/HTTPS links and normal browser intents open correctly from external apps. |
| Profile routing | External links use the default, last-active, or explicitly selected profile according to settings. |
| Android permissions | Permissions are requested only when needed and can be managed later. |
| Website permissions | Site permissions remain separate from Android permissions and profile-scoped. |
| Camera/microphone | Chromium and Android permission layers both work; denial does not crash browsing. |
| Location | Off by default and revocable. |
| Notifications | PHNX and website notification permissions remain distinct. |
| Files/downloads | Android storage rules and user-selected locations are respected. |
| Security | Dangerous schemes and untrusted intent extras are rejected safely. |
| Regression | Existing Phase 1–11 functionality and profile isolation remain intact. |

Phase 12 is complete when PHNX can be selected as Android's default browser, can receive HTTP/HTTPS links, handles external apps and profile routing correctly, exposes a permission-management UI, requests permissions contextually, separates website and Android permissions, handles camera/microphone/location/notifications/downloads safely, opens system settings, and preserves profile isolation.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Android system integration](./diagrams/rendered/android-system-integration.png)
- [Default-browser role flow](./diagrams/rendered/default-browser-role.png)
- [External-link profile routing](./diagrams/rendered/external-link-routing.png)
- [Permission layers](./diagrams/rendered/permission-layers.png)
- [Intent and URL safety](./diagrams/rendered/intent-safety.png)
