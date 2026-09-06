# PHNX Browser — Phase 11 Blueprint

## Self-Update and Patch System

> **Scope:** Build a production-grade self-update architecture for PHNX Browser that supports release discovery, delta patches, full APK fallback, cryptographic verification, staged rollout, rollback protection, failed-update recovery, and background update checking.

> **Safety rule:** The update system operates on the application, not user browser data. It must preserve profiles, sessions, cookies, storage, history, bookmarks, downloads, permissions, settings, network configuration, and other persistent user data.

## Objective and update architecture

The update path is:

```text
GitHub → release/patch server → installed PHNX → check → download
→ verify → apply safely → restart → updated PHNX
```

GitHub Actions builds, signs, verifies, and publishes artifacts. A patch/release server exposes signed metadata, full APKs, delta patches, checksums, signatures, and release-channel information. PHNX's `UpdateManager` checks metadata, downloads with policy awareness, verifies artifacts, applies them atomically, and confirms startup before marking the update successful.

## Release channels and versioning

Support `STABLE`, `BETA`, and `NIGHTLY`, with Stable as the default. Users change the channel under **Settings → About PHNX → Updates → Update Channel**. Each channel has independent metadata.

Every build includes version name, version code, build ID, Git commit, and release channel. Compare numeric `versionCode`, not only version strings, and never downgrade automatically. Example metadata:

```text
Version: 1.1.0
Version code: 11000
Build: 20260905.1432
Commit: abc1234
Channel: stable
```

## Signed update manifest

Expose strongly typed, versioned update metadata containing product, platform, architecture, channel, latest version/code, minimum supported version code, update type, delta information, full APK information, checksums, signature, and mandatory status.

```json
{
  "product": "phnx-browser",
  "platform": "android",
  "architecture": "arm64-v8a",
  "channel": "stable",
  "latestVersion": "1.1.0",
  "latestVersionCode": 11000,
  "minimumSupportedVersionCode": 10000,
  "updateType": "delta",
  "mandatory": false
}
```

The production schema must be signed, strongly typed, versioned, and validated before use.

## Delta patches and full APK fallback

Prefer a compatible delta patch to reduce bandwidth, download time, storage, and mobile-data usage. A delta is never mandatory. If the base version is unknown, the patch is incompatible/corrupt, verification fails, storage is insufficient, or patch application fails, fall back to a complete signed APK.

Every release must have a full APK available. The fallback path is:

```text
Delta unavailable or fails → download full APK → verify → install
```

The patch engine must never modify the live installation in an uncontrolled way. Use a temporary workspace, recovery information, validation, and an explicit commit step.

## Update checking and user experience

Check at app startup, periodically, manually, and after a sufficient interval since the last check. Avoid excessive checks by using ETags, `If-Modified-Since`, cached metadata, and exponential backoff.

Expose update controls under:

```text
Settings → About PHNX → Updates
```

Include automatic updates, Wi-Fi-only, background download, update channel, Check for updates, current version, and last checked. Advanced options such as Use Delta Updates and Allow Full APK Fallback appear only when they provide real value.

Respect Wi-Fi preference, metered connections, battery state, charging state, network availability, and download size. The browser should remain usable during background downloads where possible. Mandatory updates are reserved for security or compatibility requirements, not ordinary feature releases.

## Update state machine

Implement explicit states:

```text
IDLE → CHECKING → UPDATE_AVAILABLE → DOWNLOADING → DOWNLOADED
→ VERIFYING → VERIFIED → APPLYING → PENDING_RESTART → RESTARTING → UPDATED
```

Failure states include `CHECK_FAILED`, `DOWNLOAD_FAILED`, `VERIFICATION_FAILED`, `PATCH_FAILED`, `INSTALL_FAILED`, and `ROLLBACK_REQUIRED`. Every state must recover cleanly. Never display “Updated successfully” until installation and startup validation actually succeed.

## Cryptographic verification

HTTPS alone is insufficient. Every release artifact has a SHA-256 checksum and cryptographic signature. Verify the downloaded file hash, expected checksum, signature, version, package identity, architecture, and compatibility before applying it.

```text
Downloaded artifact → SHA-256 → signature → version → package identity → valid
```

On any failure, reject, do not apply, and delete the invalid artifact. The public verification key may be embedded in PHNX; private signing keys must never ship inside the application or repository.

## Patch engine and atomic updates

Create a dedicated `PatchEngine` with:

```text
validatePatch()
verifyPatch()
preparePatch()
applyPatch()
finalizePatch()
rollbackPatch()
```

Updates must be atomic: success commits the new version; failure keeps the old version. Never leave a partially updated or corrupted installation.

Before applying, record previous version, target version, update session ID, update state, and startup-success status. Mark an update complete only after the new version passes startup validation. If startup fails, detect recovery mode and roll back or provide safe recovery while preserving persistent user data.

## Staged rollout and emergency controls

Support staged rollout percentages such as 5%, 25%, 50%, and 100%. The patch server can pause a rollout after monitoring early users. It must also support disabling or revoking a release, marking a version compromised, forcing a minimum version, and changing rollout percentage. PHNX respects release revocation.

## Suggested architecture

```text
app/src/main/java/com/phoenix/phnx/
├── update/
│   ├── UpdateManager.kt
│   ├── UpdateChecker.kt
│   ├── UpdateManifest.kt
│   ├── UpdateState.kt
│   ├── UpdatePolicy.kt
│   ├── UpdateChannel.kt
│   ├── UpdateScheduler.kt
│   ├── DownloadManager.kt
│   ├── PatchEngine.kt
│   ├── PatchValidator.kt
│   ├── ArtifactVerifier.kt
│   ├── SignatureVerifier.kt
│   ├── ChecksumVerifier.kt
│   ├── VersionComparator.kt
│   ├── UpdateInstaller.kt
│   ├── RollbackManager.kt
│   ├── RecoveryManager.kt
│   └── UpdateTelemetry.kt
├── update/network/
│   ├── UpdateApi.kt
│   ├── UpdateHttpClient.kt
│   └── UpdateCache.kt
├── update/model/
│   ├── ReleaseInfo.kt
│   ├── PatchInfo.kt
│   ├── ArtifactInfo.kt
│   └── RolloutInfo.kt
└── update/ui/
    ├── UpdateSettings.kt
    ├── UpdateDialog.kt
    ├── UpdateProgress.kt
    └── UpdateStatus.kt
```

## Patch server and GitHub Actions

The patch server exposes conceptual endpoints such as:

```text
GET /api/v1/update
GET /api/v1/releases
GET /api/v1/releases/{version}
GET /api/v1/patches/{from}/{to}
GET /api/v1/artifacts/{version}
```

It does not build releases. GitHub Actions extends Phase 9:

```text
Tag → tests → release build → security checks → APK/AAB
→ SHA-256 → signing → delta generation → release manifest → publish
```

Signing secrets remain in GitHub Secrets. Never commit keystores, private signing keys, signing passwords, or API secrets.

## Integration with resources and performance

Respect Phase 5 ResourceManager and Phase 8 PerformanceManager. Avoid expensive downloads, verification, patching, and restart work while the device is overheating, battery is critically low, CPU is heavily loaded, or the browser is under heavy workload. Prefer background download, controlled verification, scheduled apply, and user-aware restart.

Update logs include current/target versions, update type, patch size, download/verification/installation results, failure reason, and update session ID. Never log private keys, signing secrets, authentication tokens, passwords, cookies, or profile data.

## Database and profile safety

Updates must preserve profile data and use versioned, transactional, recoverable, tested migrations whenever internal storage changes. Never silently delete incompatible data. Migration failure protects existing data, reports failure, and offers recovery.

## Acceptance checklist

- [ ] Installed PHNX detects new releases.
- [ ] GitHub Actions produces signed update artifacts and metadata.
- [ ] Release channels and numeric version comparison work.
- [ ] Delta patches are supported but never mandatory.
- [ ] Full APK fallback works.
- [ ] SHA-256 and cryptographic signature verification work.
- [ ] Invalid or tampered artifacts are rejected and deleted.
- [ ] Updates are atomic.
- [ ] Failed updates recover safely and rollback exists.
- [ ] Staged rollout and emergency release controls work.
- [ ] Mandatory security updates work without forcing ordinary feature updates.
- [ ] Profiles and user data survive updates.
- [ ] Background download, Wi-Fi/metered policies, and scheduling work.
- [ ] Update states and status messages are accurate.
- [ ] Logs contain no sensitive data.
- [ ] Release signing secrets never enter the repository.
- [ ] Phase 9 CI/CD and Phase 10 blocker functionality remain intact.

## End state

PHNX Browser now combines the Phase 1–9 browser foundation, profiles, network, privacy, performance, release infrastructure, and Phase 10 blocker with a secure self-update and patch system. It can detect, download, verify, apply, recover, roll back, and report updates without destroying user data or weakening release security.

## Related diagrams

Editable Mermaid sources are in [`diagrams/`](./diagrams/), and rendered PNGs are in [`diagrams/rendered/`](./diagrams/rendered/).

- [Update architecture](./diagrams/rendered/update-architecture.png)
- [Update state machine](./diagrams/rendered/update-state-machine.png)
- [Delta and full APK fallback](./diagrams/rendered/delta-fallback.png)
- [Verification and atomic apply](./diagrams/rendered/verification-atomic-apply.png)
- [Staged rollout and rollback](./diagrams/rendered/rollout-rollback.png)
