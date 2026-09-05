# PHNX Browser — Phase 2 Blueprint

## Profile System + Persistent Isolation

> **Scope:** Build the complete PHNX Profile System. Each profile is an independent browser environment with its own browser data, permissions, history, bookmarks, tabs, and runtime state. Phase 2 establishes profile creation, storage isolation, switching, persistence, lifecycle state, and profile UI.

> **Boundary:** Per-profile proxy routing, advanced fingerprint or device configuration, network fingerprinting changes, advanced RAM optimization, thermal throttling, and production anti-abuse features are intentionally deferred to later phases.

## Objective

A profile must maintain its own browser data and session without accidentally sharing data with another profile. The `ProfileManager` is the single source of truth for profile metadata; UI classes must not manipulate profile files directly.

## Profile model

Each profile contains the following conceptual areas:

| Area | Contents |
|---|---|
| Identity | Stable ID and display name |
| Browser data | Cookies, LocalStorage, IndexedDB, cache, and service workers |
| Permissions | Profile-specific site permissions where supported |
| User data | History and bookmarks |
| Runtime state | Lifecycle status and active session information |
| Tabs | Tabs owned exclusively by this profile |

Example profiles include Phoenix, Work, Shopping, and Testing. Each profile must remain independent.

## Core components

### ProfileManager

Create a `ProfileManager` with the following responsibilities:

```text
createProfile()
deleteProfile()
renameProfile()
switchProfile()
getProfile()
getAllProfiles()
saveProfile()
loadProfile()
```

The manager owns profile metadata operations and coordinates storage, browser-context creation, session restoration, and lifecycle transitions.

### Profile database

Use Room with SQLite and create a `ProfileEntity` containing at least:

| Field | Purpose |
|---|---|
| `id` | Stable unique identifier; never changes during rename |
| `name` | User-facing display name |
| `createdAt` | Creation timestamp |
| `lastUsedAt` | Last activation timestamp |
| `status` | `ACTIVE`, `IDLE`, `SUSPENDED`, or `CLOSED` |
| `storagePath` | Application-private storage location |

Example:

```text
id: profile_001
name: Phoenix
status: ACTIVE
storagePath: /profiles/profile_001/
```

### Profile storage

Every profile receives its own application-private directory. The exact Chromium storage layout may differ according to the selected integration, so the storage layer must use supported Chromium mechanisms rather than manually faking internal formats.

```text
/profiles/
├── profile_001/
│   ├── browser-data/
│   ├── cookies/
│   ├── cache/
│   ├── local-storage/
│   └── indexed-db/
├── profile_002/
│   ├── browser-data/
│   ├── cookies/
│   ├── cache/
│   ├── local-storage/
│   └── indexed-db/
└── profile_003/
```

The profile name must never be used as the storage directory identifier. Use a stable generated ID such as `profile_7d9f2c41`, so renaming changes only metadata and never breaks storage association.

## Isolation requirement

Isolation is a critical acceptance criterion. If Profile A receives a cookie, LocalStorage value, session, IndexedDB record, permission, or other browser data, Profile B must not automatically receive it.

```text
Profile A                         Profile B
example.com                       example.com
cookie = AAA                      cookie = BBB or none
LocalStorage A                    LocalStorage B
IndexedDB A                       IndexedDB B
Permissions A                     Permissions B
```

The implementation must test isolation through actual browser contexts and supported persistent-storage mechanisms. It must not simulate isolation by merely changing labels or metadata.

## Profile lifecycle

Implement the basic state machine:

```text
ACTIVE → IDLE → SUSPENDED → CLOSED
   ▲       │                    │
   └───────┴──── switch/load ───┘
```

Phase 2 requires the state model and safe transitions. Detailed RAM/CPU release behavior belongs to Phase 5.

## Default profile and creation flow

On first launch, PHNX must create a default profile named `Phoenix` before the browser opens. The browser must never launch with an undefined active profile.

When the user selects **New Profile**, show a name field with Cancel and Create actions. After creation:

```text
Profile created
      ↓
Initialize profile storage
      ↓
Create browser context
      ↓
Open new tab
```

Profile IDs must be unique and stable for the lifetime of the profile.

## Profile switcher and session persistence

Add a visible profile selector to the browser UI. The active profile must be obvious to the user, for example:

```text
PHNX
● Work
```

A switch operation should save the current state, pause or release the current runtime as appropriate, load the selected profile, and restore its session. Detailed runtime optimization may be improved in Phase 5.

Tabs belong to exactly one profile:

```text
Profile A                  Profile B
├── Tab A1                 ├── Tab B1
└── Tab A2                 └── Tab B2
```

Do not mix tabs between profiles. A profile's persistent browser data and restorable session should survive app restart, process restart, and device reboot unless the user explicitly deletes the profile or its data.

## Rename and delete behavior

Renaming changes only the display name. The profile ID, storage path, browser data, and tab relationship remain attached to the same profile.

Deletion must be deliberate and require confirmation. The confirmation must explain that browsing data will be permanently removed, including cookies, history, cache, site data, permissions, and session data. Deleting one profile must not affect any other profile.

Each profile's action menu should provide:

```text
Switch
Rename
Settings
Delete
```

## Global versus profile-specific settings

Maintain a clear distinction between application-wide settings and profile-specific settings.

| Global settings | Profile settings |
|---|---|
| App theme | Cookies |
| Search engine | Site permissions |
| General browser preferences | History |
|  | Storage |
|  | Future network configuration |
|  | Future device configuration |

The distinction is foundational for Phases 3 and 4.

## Security and crash recovery

Profile directories must remain in Android application-private storage and must not be exposed casually. Establish a secure-storage abstraction for sensitive values; future secret types should use Android Keystore. Passwords and proxy credentials must never be stored as plaintext.

If PHNX or a Chromium process crashes, profile data must remain intact. Use safe writes and transactions for profile metadata. A renderer or browser-process crash must never delete a profile.

## Profile UI

Create a dedicated `PHNX Profiles` screen showing each profile's name and lifecycle status, with a New Profile action. Each profile has an overflow menu for Switch, Rename, Settings, and Delete. The active profile must be visually distinct.

## Automated testing

Create automated tests for the following scenarios:

| Test area | Expected result |
|---|---|
| Creation | Create multiple profiles and verify every ID is unique. |
| Storage isolation | A cookie created in Profile A is absent from Profile B. |
| Persistence | Data survives app/process restart and profile reload. |
| Rename | The display name changes while ID and storage remain unchanged. |
| Delete | Metadata and storage are removed; other profiles remain unaffected. |
| Tabs | Tabs remain owned by their original profile. |
| Crash recovery | Profile data remains available after a simulated crash/reopen path. |

## Acceptance checklist

- [ ] Profile manager exists and is the metadata source of truth.
- [ ] Room/SQLite profile database exists.
- [ ] Default `Phoenix` profile is created automatically.
- [ ] Users can create, rename, switch, and delete profiles.
- [ ] Every profile has a stable unique ID.
- [ ] Every profile has independent persistent storage.
- [ ] Cookies, LocalStorage, and IndexedDB do not leak between profiles.
- [ ] Permissions are profile-specific where supported.
- [ ] Tabs belong to one profile and do not mix across profiles.
- [ ] Profile data survives app restart and device reboot.
- [ ] Profile state restores safely.
- [ ] A crash does not destroy profile data.
- [ ] Global and profile settings are separated.
- [ ] Profile UI works and clearly identifies the active profile.
- [ ] Automated isolation and lifecycle tests pass.

## Explicit exclusions

Do not implement the following in Phase 2:

- Per-profile proxy or IP routing.
- Advanced fingerprint or device configuration.
- Canvas or WebGL modification.
- Network fingerprinting changes.
- Advanced RAM optimization.
- Thermal throttling.
- Production anti-abuse or anti-fraud bypass features.

## End state

After Phase 2, PHNX should provide multiple independent browser environments, each with its own profile metadata, tabs, browser context, and persistent storage. This is the foundation required before implementing the per-profile network layer in Phase 3.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Profile architecture](./diagrams/rendered/profile-architecture.png)
- [Storage isolation](./diagrams/rendered/storage-isolation.png)
- [Profile lifecycle](./diagrams/rendered/profile-lifecycle.png)
- [Profile switching](./diagrams/rendered/profile-switching.png)
- [Testing matrix](./diagrams/rendered/testing-matrix.png)
