# PHNX Browser — Phase 5 Blueprint

## Profile Lifecycle + RAM, CPU, and Thermal Management

> **Scope:** Build a resource-management system that can maintain many saved profiles while keeping only the necessary profiles actively running. The system cooperates with Android lifecycle and resource rules; it does not claim to prevent Android from killing a process.

> **Safety boundary:** Resource management must preserve profile data and must not implement anti-bot, anti-fraud, CAPTCHA bypass, guaranteed anonymity, or other unrelated behavior.

## Objective

Dynamically manage RAM, CPU, measurable GPU use, battery, device thermal state, background activity, active tabs, and active profiles. The goal is to reduce unnecessary resource use, overheating, and Android process pressure while keeping the foreground profile responsive.

## Profile lifecycle

Implement the following persistent lifecycle states:

```text
ACTIVE → IDLE → FROZEN → SUSPENDED → CLOSED
   ▲       │       │          │
   └───────┴───────┴──────────┘
      restore when opened or selected
```

Lifecycle transitions must not destroy profile data. `CLOSED` means not running; it does not mean deleted. Only explicit user deletion removes cookies, history, bookmarks, storage, network configuration, identity configuration, or saved session data.

## Resource-management architecture

Suggested package structure:

```text
app/src/main/java/com/phoenix/phnx/
├── resources/
│   ├── ResourceManager.kt
│   ├── MemoryMonitor.kt
│   ├── CpuMonitor.kt
│   ├── BatteryMonitor.kt
│   ├── ThermalMonitor.kt
│   ├── ResourcePolicy.kt
│   ├── ProfileScheduler.kt
│   ├── ProfileLifecycleManager.kt
│   ├── ProfileResourceState.kt
│   └── ResourceEvent.kt
├── lifecycle/
│   ├── BrowserLifecycleController.kt
│   └── ProfileLifecycleState.kt
└── chromium/lifecycle/
    └── ChromiumLifecycleAdapter.kt
```

The central flow is:

```text
MemoryMonitor ─────┐
CpuMonitor ────────┤
BatteryMonitor ────┤
ThermalMonitor ────┤
                   ↓
             ResourceManager
                   ↓
            ProfileScheduler
                   ↓
         ProfileLifecycleManager
                   ↓
       ChromiumLifecycleAdapter
```

## ResourceManager

Create `ResourceManager` with responsibilities to monitor resources, evaluate profiles, apply policy, and coordinate `freezeProfile()`, `suspendProfile()`, `resumeProfile()`, and `closeProfile()` operations. It should react to resource events rather than performing expensive full scans continuously.

## Monitoring requirements

| Monitor | Required signals | Constraints |
|---|---|---|
| `MemoryMonitor` | Available RAM, measurable used RAM, app memory, pressure | Use supported Android APIs; do not rely on one hardcoded RAM threshold. |
| `CpuMonitor` | App/system CPU information where permitted | Avoid aggressive polling; monitoring must have minimal overhead. |
| `BatteryMonitor` | Percentage, charging state, battery-saver state where available | Become more conservative with background profiles at low battery. |
| `ThermalMonitor` | Supported Android thermal status | Do not invent exact temperature readings when only status is exposed. |

Use explicit pressure/state levels. Memory levels are `NORMAL`, `LOW`, `MODERATE`, `HIGH`, and `CRITICAL`. CPU levels are `LOW`, `NORMAL`, `HIGH`, and `CRITICAL`. Thermal levels are `THERMAL_NORMAL`, `THERMAL_LIGHT`, `THERMAL_MODERATE`, `THERMAL_SEVERE`, and `THERMAL_CRITICAL`.

## Resource policy and active-profile limits

Create `ResourcePolicy` to determine how aggressively background work is reduced:

| Condition | Policy direction |
|---|---|
| Normal | Keep the foreground profile fully active and limit background profiles. |
| Moderate pressure | Reduce background activity and freeze unnecessary tabs. |
| High pressure | Suspend inactive profiles and reduce background processing. |
| Critical pressure | Keep only essential foreground activity and suspend as many background profiles as safely possible. |

Saved profiles must not all remain fully active. The active limit must adapt to available RAM, CPU pressure, thermal state, battery, device capability, and profile activity rather than using one universal number.

## Profile priority and scheduling

`ProfileResourceState` should track `profileId`, lifecycle state, last active time, active tab count, foreground status, user-pinned status, and resource priority. Priority levels are `FOREGROUND`, `HIGH`, `NORMAL`, `LOW`, and `BACKGROUND`. The current profile normally receives the highest priority, followed by recently used or user-pinned profiles.

Create `ProfileScheduler` with:

```text
selectActiveProfiles()
selectProfilesToFreeze()
selectProfilesToSuspend()
selectProfilesToClose()
```

The scheduler should select lower-priority profiles first when resource pressure requires reduction.

## Lifecycle behavior

### Active

The foreground profile remains responsive, with normal browser interaction, JavaScript enabled, network activity, and Chromium processing. Resource saving must not unnecessarily degrade the active page.

### Idle

After inactivity, reduce background work, unnecessary polling, and non-essential tasks without immediately destroying the browser context.

### Frozen

Freeze or pause supported background activity while preserving tabs, navigation state, session state, profile storage, cookies, history, bookmarks, identity configuration, and network configuration.

### Suspended

Under significant pressure, save sessions, active tabs, navigation state, and selected-profile state, then release as much runtime resource as the supported Chromium lifecycle allows. Persistent profile data must remain intact.

### Closed

A closed profile is not running but retains all persistent data. The user can reopen it without data loss.

## Restoration and Chromium adapter

Restoration flow:

```text
Select profile
      ↓
Load persistent profile data
      ↓
Load network configuration
      ↓
Load identity configuration
      ↓
Restore session
      ↓
Resume Chromium context
      ↓
ACTIVE
```

Create `ChromiumLifecycleAdapter` with:

```text
prepareForIdle(profileId)
freeze(profileId)
suspend(profileId)
resume(profileId)
close(profileId)
saveSession(profileId)
restoreSession(profileId)
```

Use actual supported Chromium lifecycle mechanisms. A database status change alone is not suspension. If an operation is unsupported on the current device or build, report `Not supported on this device/build` rather than pretending it succeeded.

## Android lifecycle and pressure response

Integrate activity, application, and process lifecycle events with memory-pressure callbacks, thermal status, and battery state. When the app backgrounds, evaluate the foreground and background profiles and apply the resource policy. On resume, inspect persisted state and restore the required profile.

For memory pressure:

```text
Memory pressure
      ↓
ResourceManager
      ↓
Find lowest-priority profiles
      ↓
Freeze or suspend them
      ↓
Release runtime resources
      ↓
Keep persistent data
```

Thermal response should progress from normal operation, through reduced background activity, to suspending inactive profiles at severe or critical status. The foreground profile should remain usable whenever Android permits. High background CPU use should be attributed where possible and reduced without arbitrarily throttling the user's active page.

JavaScript remains enabled by default. Resource management should control supported browser lifecycle behavior rather than injecting scripts or disabling JavaScript globally. Suspended profiles should reduce unnecessary background network activity while preserving Phase 3 network configuration for resumption.

## Resource dashboard and modes

Add an advanced dashboard under:

```text
Settings → Performance → Resource Manager
```

Show RAM pressure, CPU state, battery state, thermal state, counts of active/idle/frozen/suspended profiles, and per-profile name, state, last-active time, tab count, and priority. Keep technical controls out of the normal three-dot browser menu.

Provide these modes, with **Balanced Mode** as the default:

| Mode | Behavior |
|---|---|
| Performance | Allow more active runtime when the device can handle it. |
| Balanced | Normal automatic lifecycle management. |
| Battery Saver | Suspend background profiles more aggressively without overriding Android restrictions. |

Manual profile controls may include Open, Resume, Freeze, Suspend, and Close. Do not expose dangerous process controls.

## Events, frequency, and persistence

Create `ResourceEvent` values such as `MEMORY_PRESSURE_CHANGED`, `CPU_PRESSURE_CHANGED`, `THERMAL_STATE_CHANGED`, `BATTERY_STATE_CHANGED`, `PROFILE_BECAME_IDLE`, `PROFILE_FROZEN`, `PROFILE_SUSPENDED`, `PROFILE_RESUMED`, and `PROFILE_CLOSED`.

Use Android-supported callbacks where available and reasonable periodic checks where necessary. Avoid 50 ms or 100 ms loops and continuous polling. The monitoring system itself must have minimal CPU and battery impact.

Persist only restoration-critical runtime state in an entity such as:

```text
ProfileRuntimeEntity
├── profileId
├── lifecycleState
├── lastActiveTime
├── lastSessionSaveTime
└── recoveryRequired
```

Current CPU/RAM metrics do not need permanent storage unless required for diagnostics.

## Crash recovery and threading

On restart, read persisted profile state, detect interrupted sessions, recover profiles safely, and restore required sessions. Do not mark every profile permanently closed without checking persisted state, and never delete data during recovery.

The `ResourceManager` must use minimal CPU and RAM, avoid unnecessary database writes, Chromium restarts, profile switching, and session serialization, and never block the UI thread. Session saving, persistence, resource analysis, database operations, suspension, and restoration must run off the Android main thread; UI updates return safely to the main thread.

## Automated testing

| Test area | Scenarios | Expected result |
|---|---|---|
| Lifecycle | ACTIVE → IDLE → FROZEN → SUSPENDED → ACTIVE; ACTIVE → CLOSED | States transition correctly without data loss. |
| Restoration | Browse, suspend, resume, verify session | Session and profile data remain available. |
| Isolation | Operate lifecycle on Profile A while Profile B exists | Profile B is not corrupted. |
| Memory pressure | NORMAL, LOW, HIGH, CRITICAL | Lowest-priority profiles are selected first. |
| Thermal | NORMAL, MODERATE, SEVERE, CRITICAL | Background profiles are reduced appropriately. |
| Battery | Enable battery saver | Background behavior becomes more conservative. |
| Crash recovery | Active profile, simulated crash, restart | Persistent data remains intact and recoverable. |
| Performance | Monitor overhead and main-thread behavior | No significant overhead or UI blocking. |

## Acceptance checklist

- [ ] Lifecycle states ACTIVE, IDLE, FROZEN, SUSPENDED, and CLOSED work correctly.
- [ ] Profiles can be restored after suspension.
- [ ] Persistent data survives lifecycle transitions.
- [ ] `ResourceManager` exists and coordinates policy.
- [ ] Memory, CPU, battery, and supported thermal monitoring work.
- [ ] Resource pressure affects background profiles.
- [ ] Foreground profile receives highest priority.
- [ ] Inactive profiles can be frozen or suspended.
- [ ] Many saved profiles do not automatically remain fully active.
- [ ] Sessions can be saved and restored.
- [ ] Android lifecycle is integrated.
- [ ] Crash recovery works safely.
- [ ] Resource monitoring has low overhead.
- [ ] Phase 2 isolation, Phase 3 networking, and Phase 4 identity remain intact.
- [ ] No profile is automatically deleted because of pressure.
- [ ] Unsupported Chromium lifecycle operations are reported honestly.
- [ ] Tests pass.

## Explicit exclusions

Do not implement advanced browser features, download/bookmark/history overhauls, advanced privacy controls, permission dashboards, content blocking, custom DNS, VPN implementation, anti-bot or anti-fraud bypass, CAPTCHA bypass, guaranteed anonymity, or claims of preventing Android process kills in Phase 5.

## End state

PHNX Browser supports many saved profiles without keeping every profile fully active. The system manages RAM, CPU, battery, thermal state, profile priority, and lifecycle while preserving persistent profile data and restoring sessions when needed.

```text
100 saved profiles
├── Profile 01 → ACTIVE
├── Profile 02 → IDLE
├── Profile 03 → FROZEN
├── Profile 04 → SUSPENDED
├── Profile 05 → CLOSED
└── Profile 100 → CLOSED
```

> **Many saved profiles ≠ many fully active profiles.**

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Resource architecture](./diagrams/rendered/resource-architecture.png)
- [Lifecycle state machine](./diagrams/rendered/lifecycle-state-machine.png)
- [Pressure-response policy](./diagrams/rendered/pressure-response.png)
- [Profile restoration](./diagrams/rendered/profile-restoration.png)
- [Resource dashboard](./diagrams/rendered/resource-dashboard.png)
