# PHNX Browser Release Setup

The normal `Android` workflow produces debug artifacts and only builds release artifacts when the repository signing secret is configured. A public production release must use the tag-triggered `Signed Release` workflow.

Configure these repository secrets before creating a `v*` tag:

- `PHNX_KEYSTORE_BASE64`: base64-encoded Android release keystore.
- `PHNX_KEYSTORE_PASSWORD`: keystore password.
- `PHNX_KEY_ALIAS`: signing key alias.
- `PHNX_KEY_PASSWORD`: signing key password.
- `PHNX_UPDATE_SIGNING_KEY_BASE64`: Base64-wrapped PKCS#8 PEM ECDSA P-256 private key for update manifests.

Private signing material must never be committed. The workflow decodes the Android keystore and the separate update-signing key only inside the runner's temporary directory, verifies the signed APK/AAB, signs the update manifest with ECDSA, computes SHA-256 checksums, and publishes the tagged release. The public update key is embedded in `UpdateSigningKey.kt`; it is not the Android release key.

The signed `phnx-update-manifest.json` is published as a GitHub Release asset beside the APK and AAB. The app rejects unsigned, malformed, or incorrectly signed manifests before use.

## Runtime release gate

Release builds derive the public SHA-256 fingerprint from the configured keystore during the build and embed only that fingerprint. On startup, non-debug builds verify the official package name, version metadata, and Android signing certificate. Missing, modified, repackaged, or differently signed production packages show `Modified or unofficial build detected` and do not start. Debug builds intentionally bypass this gate.

PHNX has no client-only premium entitlement switch. Any future premium or high-value security decision must be returned by a server and verified with a public-key signature, expiry, and revocation policy. Private signing keys, keystores, passwords, proxy credentials, API keys, and update-signing private keys remain outside the APK and repository.
