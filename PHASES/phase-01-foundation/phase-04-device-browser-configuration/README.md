# PHNX Browser — Phase 4 Blueprint

## Consistent Device and Browser Configuration

> **Scope:** Build a profile-scoped device and browser configuration system. Every profile must have its own internally coherent browser environment, with configuration values that agree with one another for privacy, compatibility, and predictable browser behavior.

> **Safety boundary:** Do not implement anti-bot bypass, anti-fraud bypass, CAPTCHA bypass, arbitrary fingerprint spoofing, guaranteed anonymity, or claims of undetectability.

## Objective

Each profile receives an independent `BrowserIdentityConfig`. Configuration is validated before application, persisted by profile ID, applied through a Chromium adapter using supported APIs, and checked at runtime. The implementation must honestly report capabilities that are supported, partially supported, or unavailable.

## Architecture

Suggested package structure:

```text
app/src/main/java/com/phoenix/phnx/
├── identity/
│   ├── BrowserIdentityConfig.kt
│   ├── DeviceProfile.kt
│   ├── DeviceProfileManager.kt
│   ├── DeviceProfileValidator.kt
│   ├── DevicePreset.kt
│   ├── LocaleConfig.kt
│   ├── TimezoneConfig.kt
│   ├── ViewportConfig.kt
│   └── ClientHintsConfig.kt
├── chromium/identity/
│   └── ChromiumIdentityAdapter.kt
└── profiles/
    └── ProfileIdentityRepository.kt
```

The identity layer must remain modular and separate from the Phase 3 `NetworkManager`. Do not combine `NetworkManager` and `DeviceProfileManager` into one large class.

## Profile-scoped identity configuration

Create `BrowserIdentityConfig` with at least the following fields:

| Area | Configuration |
|---|---|
| Browser | User-Agent and browser version |
| Platform | Platform and operating system |
| Display | Viewport width/height, screen width/height, color depth, and device scale factor |
| Localization | Language, languages, locale, and timezone |
| Device behavior | Touch support and mobile/desktop mode |
| Client hints | Supported client-hints configuration |
| Preset | Source `presetId` |

Every profile must own its configuration. Identity data must not be stored globally when it is intended to belong to an individual profile.

## Device presets

Create coherent `DevicePreset` definitions for supported categories such as Android Phone, Android Tablet, and Desktop. A preset must define compatible operating system, User-Agent, viewport, screen dimensions, device scale factor, locale, language, timezone, touch capability, mobile/desktop mode, and client hints.

Do not randomly combine unrelated values. The system must reject obvious contradictions such as an Android mobile operating system paired with desktop-only settings or impossible mobile/desktop combinations.

## Consistency validation

Create `DeviceProfileValidator`. Before any configuration is applied, it must validate the relationships below:

| Validation | Requirement |
|---|---|
| OS ↔ User-Agent | Declared operating system agrees with the User-Agent. |
| Platform ↔ OS | Platform does not contradict the operating system. |
| Mobile mode ↔ viewport | Mode agrees with the viewport configuration. |
| Screen ↔ viewport | Dimensions are valid for the selected device configuration. |
| DPR ↔ screen | Device scale factor is valid for the configuration. |
| Locale ↔ language | Locale and language preferences are compatible. |
| Timezone ↔ locale | Timezone is explicit and profile-specific where supported. |
| Touch ↔ device type | Touch capability agrees with the selected device type where exposed. |
| Client hints ↔ User-Agent | Client hints agree with the selected browser/device configuration where supported. |

If validation fails, do not apply the configuration. Return a clear validation error instead.

## Profile creation and switching

When creating a profile, create its identity configuration, select a supported default preset, validate it, persist it, and create or load the Chromium profile context. Do not copy mutable runtime state from another profile.

When switching profiles, save the current state, stop or transition the current context, load the selected profile and its identity configuration, validate it, apply it through `ChromiumIdentityAdapter`, and then resume or start the selected profile. Do not reuse the previous profile's User-Agent, locale, timezone, viewport, client hints, or browser preferences accidentally.

## Chromium identity adapter

Create `ChromiumIdentityAdapter` with responsibilities such as:

```text
applyUserAgent()
applyLocale()
applyLanguages()
applyTimezone()
applyViewport()
applyDeviceScaleFactor()
applyMobileMode()
applyClientHints()
applySupportedBrowserPreferences()
```

The adapter must use actual supported Chromium/Android APIs. It must not report successful application when a property cannot be changed per profile. Report limitations internally and expose capability status as `SUPPORTED`, `PARTIALLY_SUPPORTED`, or `NOT_SUPPORTED`.

Arbitrary JavaScript patches for navigator, Canvas, WebGL, audio, fonts, or WebRTC are explicitly disallowed. The goal is coherent and supportable configuration, not making profiles appear undetectable.

## Persistence and ownership

Persist identity configuration in a profile-owned entity such as `ProfileIdentityEntity` with fields including `profileId`, `presetId`, User-Agent, platform, operating system, viewport and screen dimensions, device scale factor, locale, language, languages, timezone, touch support, mobile mode, client hints, and `updatedAt`.

The profile ID is the ownership boundary. A profile must never load another profile's configuration.

## Profile editor UI

Expose advanced identity configuration through Settings:

```text
Settings → Profiles → Select Profile → Device & Browser Configuration
```

The editor should show device profile, browser, operating system, screen/viewport, scale factor, language, locale, timezone, mobile/desktop mode, and client hints. Provide Preset, Custom, Reset to Default, Save, and Validate actions. Do not expose unnecessary technical settings in the normal three-dot browser menu.

## Presets and configuration snapshots

Create `DeviceProfileManager` with methods including:

```text
getAvailablePresets()
getPreset(id)
applyPreset(profileId, presetId)
getProfileConfiguration(profileId)
updateProfileConfiguration(profileId, config)
validateProfileConfiguration(profileId)
resetProfileConfiguration(profileId)
```

Applying a preset must be atomic from the user's perspective: load the preset, generate the complete configuration, validate it, save it, and apply it to Chromium. Never partially apply a preset.

Before launching or resuming a profile, generate a `ProfileConfigurationSnapshot` containing browser, OS, platform, User-Agent, viewport, screen, DPR, locale, languages, timezone, touch, and client hints. Snapshots must never include passwords, proxy credentials, cookies, or other secrets.

## Runtime consistency checking

Create `IdentityRuntimeChecker` to compare expected configuration values with what PHNX actually applied through supported Chromium APIs. A mismatch should produce an `IDENTITY_CONFIGURATION_MISMATCH` diagnostic without exposing sensitive data.

## Integration with profile and network layers

The identity configuration follows the Phase 2 and Phase 3 isolation model:

```text
Profile A                    Profile B
├── Storage A                ├── Storage B
├── Cookies A                ├── Cookies B
├── Network A                ├── Network B
└── Identity A               └── Identity B
```

The profile manager owns profile scope, while `DeviceProfileManager` and `NetworkManager` remain separate modular services. Identity and network layers may coordinate during profile switching, but neither should absorb the responsibilities of the other.

## Secure data and logging

Identity configuration must not contain passwords, proxy passwords, API keys, or authentication tokens. Sensitive configuration belongs to the secure credential system established in Phase 3.

Logs may contain profile ID, preset ID, configuration status, validation result, and apply result. Never log cookies, passwords, proxy credentials, authentication tokens, private browsing data, or sensitive browsing content.

## Performance

Identity configuration should be lightweight. Apply it at the correct browser/profile lifecycle boundary rather than rebuilding the entire browser environment on every page navigation. Avoid unnecessary runtime JavaScript injection.

## Automated testing

| Test area | Expected result |
|---|---|
| Configuration | Create, update, reset, persist, and load identity configuration. |
| Isolation | Profile A and Profile B retain different configurations. |
| Validation | Valid values pass; invalid User-Agent, viewport, DPR, locale, timezone, client hints, and contradictory mobile/desktop combinations fail safely. |
| Switching | A → B → A restores each profile's own configuration. |
| Persistence | Configuration survives app close, restart, and profile reload. |
| Chromium application | Every supported property is verified as actually applied, not merely saved to SQLite. |
| Capability reporting | Unsupported properties are reported honestly. |

## Acceptance checklist

- [ ] Every profile has independent device/browser configuration.
- [ ] Supported device presets work.
- [ ] Custom configuration can be saved.
- [ ] Configuration is validated before application.
- [ ] User-Agent and OS remain consistent.
- [ ] Platform and OS remain consistent.
- [ ] Viewport and screen configuration remain consistent.
- [ ] DPR is validated.
- [ ] Locale and language configuration work.
- [ ] Timezone is profile-specific where supported.
- [ ] Mobile/desktop mode is profile-specific where supported.
- [ ] Client hints are configured only where supported.
- [ ] Configuration persists after restart.
- [ ] Switching profiles restores the correct configuration.
- [ ] Profiles cannot inherit another profile's configuration.
- [ ] Chromium application status can be verified.
- [ ] Unsupported capabilities are reported honestly.
- [ ] No arbitrary fingerprint API manipulation is implemented.
- [ ] No anti-bot or anti-fraud bypass is implemented.
- [ ] No sensitive credentials appear in logs.
- [ ] Unit and integration tests pass.
- [ ] Existing Phase 1–3 functionality remains working.

## Explicit exclusions

Do not implement RAM optimization, CPU throttling, thermal management, profile freezing or suspension, background resource scheduling, advanced battery optimization, Canvas/WebGL/audio/font fingerprint spoofing, anti-bot bypass, anti-fraud bypass, CAPTCHA bypass, or guaranteed undetectability in Phase 4.

## End state

At the end of Phase 4, PHNX Browser has a browser shell, multiple persistent profiles, per-profile network configuration, and per-profile device/browser configuration. Each profile is a separate browser environment with its own supported identity, storage, history, cookies, and network configuration.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Identity architecture](./diagrams/rendered/identity-architecture.png)
- [Configuration validation](./diagrams/rendered/configuration-validation.png)
- [Profile switching](./diagrams/rendered/profile-switching.png)
- [Capability reporting](./diagrams/rendered/capability-reporting.png)
- [Profile end state](./diagrams/rendered/profile-end-state.png)
