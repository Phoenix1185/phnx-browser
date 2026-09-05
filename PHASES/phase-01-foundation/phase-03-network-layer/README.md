# PHNX Browser — Phase 3 Blueprint

## Per-Profile Network Layer

> **Scope:** Implement a modular network architecture in which every browser profile has its own network configuration without accidentally sharing network state with another profile.

> **Safety boundary:** This phase is for legitimate profile isolation and privacy. It must not implement anti-bot, anti-fraud, access-control bypass, guaranteed anonymity, or “undetectable” browsing mechanisms.

## Objective

The application must never assume that all profiles use the same network configuration. Network state belongs to a profile ID and is applied through a Chromium adapter rather than being scattered throughout the UI or represented by one global proxy object.

```text
Profile
   ↓
ProfileNetworkConfig
   ↓
NetworkManager
   ↓
ChromiumNetworkAdapter
   ↓
Chromium network implementation
```

## Core components

| Component | Responsibility |
|---|---|
| `NetworkManager` | Set, retrieve, clear, test, and report the active profile's network configuration. |
| `ProfileNetworkConfig` | Store profile-linked mode, proxy type, endpoint, enablement, and credential reference. |
| `NetworkMonitor` | Observe connection state, detect Android network changes, and report transitions. |
| `ProxyManager` | Coordinate supported proxy configuration behavior. |
| `ConnectionTester` | Verify that a configured route can establish a connection. |
| `SecureCredentialStore` | Store sensitive proxy credentials through Android Keystore-backed storage. |
| `ChromiumNetworkAdapter` | Isolate Chromium-specific network integration behind a stable application interface. |
| `WebRtcNetworkPolicy` | Represent only WebRTC policies actually supported by the integrated Chromium build. |

Suggested package structure:

```text
network/
├── NetworkManager.kt
├── NetworkMonitor.kt
├── ProfileNetworkConfig.kt
├── ProxyConfig.kt
├── ProxyManager.kt
├── ConnectionTester.kt
├── WebRtcNetworkPolicy.kt
└── SecureCredentialStore.kt

chromium/network/
└── ChromiumNetworkAdapter.kt
```

## Network configuration model

Create `ProfileNetworkConfig` with at least the following fields:

| Field | Purpose |
|---|---|
| `id` | Unique configuration identifier |
| `profileId` | Profile to which this configuration belongs |
| `mode` | `DIRECT` or `PROXY` |
| `proxyType` | Explicit supported type such as HTTP, HTTPS, SOCKS4, or SOCKS5 |
| `proxyHost` | Proxy endpoint host |
| `proxyPort` | Proxy endpoint port |
| `username` | Optional proxy username |
| `credentialReference` | Identifier for secure credential storage |
| `enabled` | Whether the configuration is active |

Only expose proxy types that the actual Chromium/network implementation supports. A newly created profile must default to `DIRECT`; traffic must never be routed through a proxy silently.

## Profile separation and configuration leakage

Example profile assignments may be:

```text
Profile A → Direct
Profile B → SOCKS5 proxy
Profile C → Direct
```

Configuration identity must remain tied to profile identity:

```text
profile_001 → network_config_001
profile_002 → network_config_002
```

When switching from Profile A to Profile B, stop or transition the current browser session, safely flush Profile A's state, apply Profile B's configuration, then start or resume Profile B. The implementation must verify actual network behavior; changing a UI preference alone is not sufficient.

## Proxy configuration UI

Expose the network settings through the profile context:

```text
Settings → Profile → Network
```

The screen should provide Direct/Proxy selection, supported proxy type, host, port, username, masked password input, Test connection, and Save actions. Error messages and logs must not reveal credentials.

## Secure credentials

Never store a proxy password as plaintext in the normal profile database. The database stores only a credential reference; the secret is managed by `SecureCredentialStore` backed by Android Keystore where appropriate.

```text
Proxy configuration
       ↓
Credential reference
       ↓
Android Keystore-backed secure storage
```

Provide an abstraction such as:

```text
saveCredential()
getCredential()
deleteCredential()
```

## Connection testing and status

`Test connection` must validate that the configured route can establish a connection and display a clear success or failure state. Failure guidance may mention the host, port, credentials, or underlying network, but must not include secret values.

The profile UI should show the current connection state, mode, proxy configuration state, and last connection-test result. Use explicit states such as `CONNECTED`, `CONNECTING`, `DISCONNECTED`, and `ERROR`.

## DNS and WebRTC policy

The network abstraction must accurately represent whether DNS is direct, proxy-resolved, or configured through a supported Chromium mechanism. Do not fabricate DNS information.

Create `WebRtcNetworkPolicy` with only supported options, such as `DEFAULT`, `LIMIT_NON_PROXY_NETWORK`, and `DISABLED`. The priority is preventing accidental network exposure, not claiming that all WebRTC traffic is routed through a proxy.

## Android lifecycle and monitoring

`NetworkMonitor` should expose:

```text
observeConnection()
detectNetworkChanges()
reportConnectionState()
notifyProfileManager()
```

Handle Wi-Fi/mobile-data transitions, airplane mode, disconnection, restoration, app backgrounding, and app resume without crashing. A normal connection error should be shown when the network is unavailable, and the browser should recover its state when connectivity returns.

## Safe logging

Debug logs may report redacted operational state, for example:

```text
Profile profile_001
Network state: CONNECTED
Profile profile_002
Proxy test: SUCCESS
```

Never log proxy passwords, session cookies, authentication tokens, or full private URLs containing credentials. Use redaction consistently in debug builds and production diagnostics.

## Automated testing

| Test | Expected result |
|---|---|
| Configuration isolation | Profile A can be Direct while Profile B is Proxy, with separate persisted configurations. |
| Persistence | A profile's network configuration is restored after app close and reopen. |
| Profile switching | A → B → C → A always applies the correct configuration. |
| Invalid proxy | Invalid host, port, or credentials fail gracefully without an app crash. |
| Network transition | Monitor reports Connected → Disconnected → Connected accurately. |
| Credential security | Proxy passwords are absent from the normal profile database and logs. |
| Adapter boundary | Chromium-specific code is exercised through `ChromiumNetworkAdapter`. |

## Acceptance checklist

- [ ] `NetworkManager` exists.
- [ ] Each profile has its own network configuration.
- [ ] Direct connection works.
- [ ] Supported proxy types work.
- [ ] Proxy configuration UI works.
- [ ] Proxy credentials use secure storage.
- [ ] Proxy connection testing works.
- [ ] Network status is displayed.
- [ ] Network changes are detected.
- [ ] Profile switching applies the selected configuration.
- [ ] Configuration survives app restart.
- [ ] Configuration does not leak between profiles.
- [ ] Credentials do not appear in logs.
- [ ] Automated network tests pass.
- [ ] Chromium-specific network code is isolated behind an adapter.

## Explicit exclusions

Do not implement advanced fingerprint modification, Canvas/WebGL spoofing, audio or font fingerprint manipulation, anti-bot or anti-fraud bypass, guaranteed IP anonymity, claims of undetectable browsing, RAM/CPU lifecycle optimization, or thermal optimization in Phase 3. These are intentionally separated into later phases or excluded entirely.

## End state

At the end of Phase 3, each profile should have an independently managed network configuration that can be persisted, tested, monitored, and applied through the Chromium adapter. Profile state and network configuration must remain separate and independently managed before Phase 4 adds consistent device/browser configuration.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Network architecture](./diagrams/rendered/network-architecture.png)
- [Profile network isolation](./diagrams/rendered/profile-network-isolation.png)
- [Profile switch sequence](./diagrams/rendered/profile-switch-sequence.png)
- [Secure credential flow](./diagrams/rendered/secure-credential-flow.png)
- [Network monitor states](./diagrams/rendered/network-monitor-states.png)
